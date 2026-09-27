#!/usr/bin/env bash
set -euo pipefail

AMBIENTE="${1:-}"
PROJETO="${PROJETO:-servicetrack}"
SERVICO="${SERVICO:-catalogo}"
REGIAO="${AWS_REGION:-us-east-1}"
NAMESPACE="${NAMESPACE:-service-track-catalogo}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ "$AMBIENTE" != "local" && "$AMBIENTE" != "hml" && "$AMBIENTE" != "prd" ]]; then
  echo "uso: $0 <local|hml|prd>" >&2
  exit 1
fi

if [[ "$AMBIENTE" == "local" ]]; then
  PG_HOST="service-track-catalogo-postgres"
  PG_USER="st_cat_user"
  PG_SENHA="st_cat_local"
  PG_BANCO="st_cat"
else
  ler() {
    aws ssm get-parameter \
      --name "/$PROJETO/$AMBIENTE/$SERVICO/db/$1" \
      --with-decryption \
      --region "$REGIAO" \
      --query Parameter.Value \
      --output text
  }
  PG_HOST="$(ler endpoint)"
  PG_USER="$(ler username)"
  PG_SENHA="$(ler password)"
  PG_BANCO="$(ler name)"
fi

aplicar_sql() {
  local arquivo="$1"
  echo "postgres <- $(basename "$arquivo")"
  kubectl run "baseline-pg-$RANDOM" \
    --namespace "$NAMESPACE" \
    --rm --stdin --quiet --restart=Never \
    --image=postgres:16 \
    --env "PGPASSWORD=$PG_SENHA" \
    --command -- psql -h "$PG_HOST" -U "$PG_USER" -d "$PG_BANCO" -v ON_ERROR_STOP=1 -f - \
    < "$arquivo"
}

aplicar_js() {
  local arquivo="$1"
  local pod
  pod="$(kubectl get pod -n "$NAMESPACE" -l app=service-track-catalogo-mongo -o name | head -1)"
  if [[ -z "$pod" ]]; then
    echo "nenhum pod do mongo em $NAMESPACE" >&2
    exit 1
  fi
  echo "mongo <- $(basename "$arquivo")"
  kubectl exec -i -n "$NAMESPACE" "$pod" -- mongosh "mongodb://localhost:27017/ST_INS" --quiet < "$arquivo"
}

for arquivo in "$RAIZ"/db/postgres/*.sql; do
  aplicar_sql "$arquivo"
done

for arquivo in "$RAIZ"/db/mongo/*.js; do
  aplicar_js "$arquivo"
done

echo "baseline de $AMBIENTE aplicado. Reexecutar e seguro: os scripts sao idempotentes."
