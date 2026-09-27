#!/usr/bin/env bash
set -euo pipefail

AMBIENTE="${1:-}"
PROJETO="${PROJETO:-servicetrack}"
SERVICO="${SERVICO:-catalogo}"
REGIAO="${AWS_REGION:-us-east-1}"
NAMESPACE="${NAMESPACE:-service-track-catalogo}"
SEGREDO="service-track-catalogo-db"

if [[ "$AMBIENTE" != "hml" && "$AMBIENTE" != "prd" ]]; then
  echo "uso: $0 <hml|prd>" >&2
  exit 1
fi

ler() {
  aws ssm get-parameter \
    --name "/$PROJETO/$AMBIENTE/$SERVICO/db/$1" \
    --with-decryption \
    --region "$REGIAO" \
    --query Parameter.Value \
    --output text
}

URL="$(ler url)"
USUARIO="$(ler username)"
SENHA="$(ler password)"

kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic "$SEGREDO" \
  --namespace "$NAMESPACE" \
  --from-literal=ST_CAT_DB_URL="$URL" \
  --from-literal=ST_CAT_DB_USER="$USUARIO" \
  --from-literal=ST_CAT_DB_PASSWORD="$SENHA" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "secret $SEGREDO aplicado em $NAMESPACE a partir do SSM de $AMBIENTE"
