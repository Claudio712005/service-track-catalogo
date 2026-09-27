# Ambiente local em Docker Compose

Sobe o serviço com as três dependências que ele realmente tem: Postgres `st_cat`,
MongoDB `ST_INS` e um broker Kafka de nó único. É o caminho mais curto para exercitar o
fluxo de estoque ponta a ponta sem cluster, sem AWS e sem credencial nenhuma.

```bash
docker compose up -d --build
```

A primeira subida constrói a imagem (Gradle e treino de CDS dentro do Dockerfile), então
demora. As seguintes reaproveitam a imagem.

| Serviço | Porta no host | Para que |
|---|---|---|
| `catalogo` | 8080 | API e `/actuator/health` |
| `postgres` | 5432 | `psql` direto, quando precisar olhar a razão de estoque |
| `mongo` | 27017 | `mongosh`, para ver categoria e insumo |
| `kafka` | 29092 | produzir comando e ler evento **do host** |
| `grafana` | 3000 | painéis, alertas e os traces (`admin` / `admin`) |
| `prometheus` | 9090 | métrica coletada da aplicação |
| `tempo` | 3200 | traces |
| `loki` | 3100 | log com o trace dentro |
| `alertas` | 8088 | recebedor de webhook, para ver o alerta chegar |

A porta do broker é **29092 no host** e `kafka:9092` dentro da rede do Compose. São dois
listeners no mesmo broker: sem isso, o endereço anunciado serve a um dos dois lados e
quebra o outro.

## O schema entra pelos mesmos arquivos de `db/`

`db/postgres` e `db/mongo` são montados em `/docker-entrypoint-initdb.d` de cada banco. O
Postgres roda os `.sql` e o Mongo roda os `.js` na primeira subida, em ordem alfabética:
baseline e depois seed. Não há cópia do SQL dentro do Compose — a fonte continua sendo
`db/`, a mesma que o ritual de ambiente aplica em `hml` e `prd`.

Consequência: **o init só roda com volume vazio**. `docker compose down` preserva os
volumes, e a próxima subida encontra o schema já criado — que é exatamente o que
`ddl-auto: validate` exige. Para voltar ao estado de fábrica:

```bash
docker compose down -v
```

## Exercitar o fluxo de estoque

Os tópicos são criados pelo serviço `topicos`, que roda uma vez e sai; o `catalogo` só
começa depois que ele termina com sucesso. Auto-criação de tópico está **desligada** de
propósito: tópico que nasce por acidente do primeiro consumidor nasce com partição e
retenção que ninguém escolheu.

```bash
INSUMO=$(curl -s localhost:8080/insumos | python3 -c 'import json,sys; print(json.load(sys.stdin)[0]["id"])')
ORDEM=$(uuidgen | tr 'A-Z' 'a-z')

printf '%s:{"idMensagem":"%s","tipo":"RegistrarEntradaDeEstoque","versao":1,"ocorridoEm":"2026-09-27T15:00:00Z","dados":{"insumoId":"%s","quantidade":30}}\n' \
  "$ORDEM" "$(uuidgen | tr 'A-Z' 'a-z')" "$INSUMO" \
  | docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh \
      --bootstrap-server kafka:9092 --topic servicetrack.estoque.comandos.v1 \
      --property parse.key=true --property key.separator=:

curl -s "localhost:8080/insumos/$INSUMO/estoque"

docker compose exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:9092 --topic servicetrack.estoque.eventos.v1 \
  --from-beginning --property print.headers=true --timeout-ms 8000
```

O contrato das mensagens, com envelope e regras de idempotência, está em
[`mensageria/README.md`](mensageria/README.md). A pilha de observabilidade que sobe junto —
painéis, alertas e a correlação entre métrica, trace e log — está em
[`observabilidade.md`](observabilidade.md).

## Olhar o estado por dentro

```bash
docker compose exec postgres psql -U st_cat_user -d st_cat \
  -c "select tipo, quantidade, saldo_disponivel_apos from catalogo.estoque_movimentos order by data_criacao"

docker compose exec postgres psql -U st_cat_user -d st_cat \
  -c "select tipo_evento, data_publicacao from catalogo.outbox order by data_criacao"

docker compose exec mongo mongosh ST_INS --quiet \
  --eval 'db.CATEGORIAS.find({}, {CODIGO: 1, ATIVA: 1}).toArray()'

docker compose logs -f catalogo
```

`data_publicacao` nulo no outbox significa evento ainda não publicado. Derrubar o broker
(`docker compose stop kafka`) e registrar uma reserva é a forma mais rápida de ver isso: o
evento fica pendente, a API continua respondendo, e a próxima passagem do publicador drena
a fila quando o broker volta.

## Isto não é como `hml` e `prd` funcionam

Aqui os três bancos são contêineres descartáveis com senha literal no arquivo. Em `hml` e
`prd`, o Postgres é RDS provisionado por [`infra/terraform`](../infra/README.md), a
credencial vem do SSM para um Secret do Kubernetes, o baseline é aplicado pelo ritual de
ambiente, e a mensageria fica desligada por flag enquanto o broker da plataforma não
existir. O Compose serve ao desenvolvimento e à demonstração local — não é o desenho de
ambiente gerenciado.
