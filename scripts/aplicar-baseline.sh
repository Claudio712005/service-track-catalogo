#!/usr/bin/env bash
set -euo pipefail

AMBIENTE="${1:-}"
PROJETO="${PROJETO:-servicetrack}"
SERVICO="${SERVICO:-catalogo}"
REGIAO="${AWS_REGION:-us-east-1}"
NAMESPACE="${NAMESPACE:-service-track-catalogo}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ESPERA_DO_MONGO="${ESPERA_DO_MONGO:-120}"
MONGO_OPCIONAL="${MONGO_OPCIONAL:-0}"

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

kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f - >/dev/null

pod_do_mongo() {
  local limite=$((SECONDS + ESPERA_DO_MONGO)) pod=""
  while [[ -z "$pod" && $SECONDS -lt $limite ]]; do
    pod="$(kubectl get pod -n "$NAMESPACE" -l app=service-track-catalogo-mongo \
      --field-selector status.phase=Running -o name 2>/dev/null | head -1)"
    [[ -z "$pod" ]] && sleep 5
  done
  echo "$pod"
}

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
  local arquivo="$1" pod="$2"
  echo "mongo <- $(basename "$arquivo")"
  kubectl exec -i -n "$NAMESPACE" "$pod" -- mongosh "mongodb://localhost:27017/ST_INS" --quiet < "$arquivo"
}

for arquivo in "$RAIZ"/db/postgres/*.sql; do
  aplicar_sql "$arquivo"
done

POD_MONGO="$(pod_do_mongo)"

if [[ -z "$POD_MONGO" ]]; then
  if [[ "$MONGO_OPCIONAL" == "1" ]]; then
    echo "AVISO: nenhum pod do mongo em $NAMESPACE depois de ${ESPERA_DO_MONGO}s."
    echo "AVISO: o Postgres foi aplicado. Rode este script de novo quando o ArgoCD tiver sincronizado."
    exit 0
  fi
  echo "nenhum pod do mongo em $NAMESPACE depois de ${ESPERA_DO_MONGO}s" >&2
  exit 1
fi

for arquivo in "$RAIZ"/db/mongo/*.js; do
  aplicar_js "$arquivo" "$POD_MONGO"
done

echo "baseline de $AMBIENTE aplicado. Reexecutar e seguro: os scripts sao idempotentes."
