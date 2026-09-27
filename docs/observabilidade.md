# Observabilidade

Os três sinais, cada um no seu lugar, e ligados entre si: **métrica** diz que algo está
errado, **trace** diz onde, **log** diz o quê. Sem a ligação, cada um deles é uma ilha e a
investigação vira adivinhação.

```
                 raspa /actuator/prometheus
Prometheus  <────────────────────────────────  catálogo
    ▲  ▲                                          │  agente OpenTelemetry
    │  └── remote write (métrica derivada de span) │
    │                                              ├── traces  ──► Tempo  (OTLP gRPC 4317)
  Grafana ──► Tempo, Loki, Prometheus              └── logs    ──► Loki   (OTLP HTTP /otlp/v1/logs)
    │
    └── alerta ──► webhook (contêiner alertas)
```

| Serviço | Porta | O que faz |
|---|---|---|
| Grafana | 3000 | painéis, alertas e a navegação entre os três sinais (`admin` / `admin`) |
| Prometheus | 9090 | coleta e guarda métrica; recebe a métrica que o Tempo deriva dos spans |
| Tempo | 3200, 4317, 4318 | guarda trace e gera métrica de span (RED e grafo de serviço) |
| Loki | 3100 | guarda log, recebido por OTLP com o trace já dentro |
| alertas | 8088 | recebedor de webhook que imprime a notificação, para ver o alerta chegar |

Métrica é **puxada** (Prometheus raspa a aplicação); trace e log são **empurrados** pelo
agente. A aplicação não depende de nenhum dos quatro para subir — telemetria fora do ar não
pode derrubar serviço, por isso nenhum deles está no `depends_on` do `catalogo`.

## A correlação, em três cliques

1. No painel **Catálogo — mensageria e estoque**, o painel de log mostra a linha com
   `trace_id`, `correlationId` e `transactionId`.
2. O `trace_id` é um link: abre o trace no Tempo, com os spans de HTTP, JDBC, Mongo e Kafka.
3. No trace, **Logs for this span** volta ao Loki filtrando por aquele trace.

Isso funciona porque o agente injeta `trace_id` no MDC do Logback e exporta o log para o
Loki como metadado estruturado. O padrão de log também carrega os três identificadores, então
`docker compose logs catalogo` mostra a mesma correlação em texto puro:

```
[service-track-catalogo,b38ab8e91d451a12d2dc2d02a2a0982b,obs-recusa,a91cc49a-...]
```

`correlationId` é nosso, atravessa a fila no envelope da mensagem e no cabeçalho
`X-Correlation-Id`; `trace_id` é do OpenTelemetry e atravessa a fila no `traceparent`, que o
agente escreve e lê sozinho. São coisas diferentes de propósito: o primeiro identifica o
atendimento do ponto de vista do negócio, o segundo a execução técnica.

## Métricas que só existem porque foram instrumentadas

Framework entrega HTTP, JVM, Hikari e cliente Kafka. O que importa neste serviço não vem de
graça:

| Métrica | Por que existe |
|---|---|
| `catalogo_outbox_pendentes` | evento gravado e não publicado. **É o sintoma mais importante**, porque a falha do outbox não gera erro: gera silêncio e dado errado do outro lado |
| `catalogo_estoque_comandos_total{tipo,resultado}` | separa `aplicado`, `ja_processado` (reentrega barrada pela idempotência), `recusado` (regra de negócio) e `invalido` (foi para a DLT) |
| `catalogo_estoque_eventos_publicados_total` | vazão de saída do outbox, para comparar com o pendente |
| `catalogo_estoque_reservas_expiradas_total` | quantas reservas a saga abandonou e a rotina devolveu ao estoque |

As séries nascem **em zero na subida**, com todas as combinações de rótulo previstas. Não é
detalhe: contador que aparece só no primeiro incremento faz `increase()` devolver zero na
primeira ocorrência, e o alerta que depende dele não dispara justamente na hora em que
deveria. Cardinalidade fica limitada porque `tipo` desconhecido é normalizado para
`desconhecido` antes de virar rótulo — mensagem estranha na fila não pode inventar série nova.

## Alertas

Sete regras provisionadas em `observabilidade/grafana/provisioning/alerting/`, todas com
`summary` e `description` que dizem o que olhar primeiro.

| Alerta | Condição | Severidade |
|---|---|---|
| Catálogo fora do ar | `up == 0` por 1 min, e **sem dado também alerta** | crítica |
| Taxa de erro 5xx acima de 5% | proporção de `SERVER_ERROR` em 5 min | alta |
| Latência p95 acima de 1s | `histogram_quantile(0.95, ...)` em 5 min | média |
| Outbox acumulando evento pendente | pendentes > 20 por 2 min | alta |
| Comando descartado para a DLT | qualquer aumento de `resultado="invalido"` em 10 min | alta |
| Consumidor atrasado na fila | lag > 100 por 5 min | média |
| Pool de conexão saturado | `hikaricp_connections_pending > 0` por 2 min | alta |

Notificação vai para um contato do tipo webhook. Severidade `critica` tem rota própria, sem
espera de agrupamento e com repetição a cada 15 min. Localmente o recebedor é o contêiner
`alertas`, que imprime o corpo da notificação:

```bash
docker compose logs -f alertas
```

Em ambiente de verdade esse contato passa a ser o canal do plantão. O que **não** muda é o
resto: as regras, os limites e as anotações são as mesmas, versionadas aqui.

### Provar que um alerta funciona

Alerta que ninguém viu disparar é decoração. Os dois caminhos mais rápidos:

```bash
docker compose stop catalogo
```

Em cerca de 1 min, **Catálogo fora do ar** fica `firing` (é o caso de "sem dado", que também
alerta) e a notificação aparece no log do `alertas`. Subir de volta resolve o alerta, e a
resolução também é notificada.

```bash
ORDEM=$(uuidgen | tr 'A-Z' 'a-z')
printf '%s:{"idMensagem":"%s","tipo":"NaoExiste","versao":1,"ocorridoEm":"2026-01-01T00:00:00Z","dados":{}}\n' \
  "$ORDEM" "$(uuidgen | tr 'A-Z' 'a-z')" \
  | docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh \
      --bootstrap-server kafka:9092 --topic servicetrack.estoque.comandos.v1 \
      --property parse.key=true --property key.separator=:
```

Comando de tipo desconhecido vai para a DLT e faz **Comando descartado para a DLT** disparar
na avaliação seguinte.

## Painéis

Dois, provisionados a partir de `observabilidade/grafana/dashboards/`:

- **Catálogo — visão geral**: estado, requisições por segundo e por rota, erro 5xx, latência
  por percentil, memória e coleta de lixo da JVM, threads, e o pool de conexão com o teto
  desenhado como linha tracejada.
- **Catálogo — mensageria e estoque**: pendentes no outbox, atraso do consumidor, comandos por
  tipo e por desfecho, eventos publicados, métrica derivada dos spans, log do serviço e lista
  de traces recentes.

Editar pelo navegador é permitido, mas a edição **não sobrevive** a um `docker compose down`:
o provisionamento reaplica o arquivo. Mudança que deve durar vai no JSON do repositório.

## Detalhes que confundem se ninguém avisar

- **O `job` do Prometheus é `catalogo`**, e quase toda consulta filtra por ele. Renomear o job
  no `prometheus.yml` quebra os painéis e os alertas de uma vez.
- **`/actuator/prometheus` só existe** porque `micrometer-registry-prometheus` está no
  classpath. Sem ele o endpoint devolve 404 mesmo estando listado em
  `management.endpoints.web.exposure.include` — foi exatamente o que aconteceu aqui antes de
  a dependência entrar.
- **A imagem imprime `Mismatched values for property jdk.module.addmods` na subida** quando o
  agente está ligado. O arquivo CDS é treinado sem o agente, e o módulo `java.instrument` que
  o `-javaagent` acrescenta não casa com o do treino. O compartilhamento de classes continua
  valendo — medido em 5,9s contra 7,2s de refresh sem CDS.
- **O endpoint `metrics` do Actuator está exposto no Compose** (não em `hml`/`prd`). Serve para
  achar o medidor que quebrou uma coleta: `curl localhost:8080/actuator/metrics/<nome>`.
- **Métrica do broker não é coletada.** Lag e taxa vêm do cliente dentro da aplicação, não do
  Kafka. Para métrica do broker seria preciso um exportador JMX ao lado dele, que só se
  justifica quando o broker for gerenciado de verdade.
- **Nada disto está ligado em `hml` e `prd` ainda.** A Fase 3 usava Datadog, que saiu; o
  desenho aqui é a base do que entra no lugar. A decisão de onde roda a pilha em ambiente
  efêmero — e quanto ela custa numa conta AWS Academy — não está tomada.
