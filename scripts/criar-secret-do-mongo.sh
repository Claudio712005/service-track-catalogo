#!/usr/bin/env bash
set -euo pipefail

AMBIENTE="${1:-}"
PROJETO="${PROJETO:-servicetrack}"
SERVICO="${SERVICO:-catalogo}"
REGIAO="${AWS_REGION:-us-east-1}"
NAMESPACE="${NAMESPACE:-service-track-catalogo}"
SEGREDO="service-track-catalogo-mongo"

if [[ "$AMBIENTE" != "hml" && "$AMBIENTE" != "prd" ]]; then
  echo "uso: $0 <hml|prd>" >&2
  exit 1
fi

ler() {
  local caminho="/$PROJETO/$AMBIENTE/$SERVICO/mongodb/$1"
  if ! aws ssm get-parameter --name "$caminho" --with-decryption --region "$REGIAO" \
      --query Parameter.Value --output text 2>/dev/null; then
    echo "::error::$caminho nao existe no SSM." >&2
    echo "O cluster do MongoDB Atlas nao e criado por Terraform: ele sobrevive ao destroy." >&2
    echo "Cadastre uma vez por ambiente: uri, username e password." >&2
    exit 1
  fi
}

URI="$(ler uri)"
USUARIO="$(ler username)"
SENHA="$(ler password)"

if [[ "$URI" != mongodb+srv://* && "$URI" != mongodb://* ]]; then
  echo "::error::a uri do Atlas precisa comecar com mongodb+srv:// ou mongodb://" >&2
  exit 1
fi

if [[ "$URI" == *"@"* ]]; then
  echo "::error::a uri nao deve carregar credencial: usuario e senha entram separados," >&2
  echo "::error::para a senha nao viajar dentro da uri nem aparecer em log do driver." >&2
  exit 1
fi

kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f - >/dev/null

kubectl create secret generic "$SEGREDO" \
  --namespace "$NAMESPACE" \
  --from-literal=MONGODB_URI="$URI" \
  --from-literal=MONGODB_USERNAME="$USUARIO" \
  --from-literal=MONGODB_PASSWORD="$SENHA" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "secret $SEGREDO aplicado em $NAMESPACE a partir do SSM de $AMBIENTE"
