# Mensageria do catálogo

O estoque deste serviço muda por **comando recebido do Kafka**, não por chamada HTTP. Reserva,
consumo e liberação são passos da saga da ordem de serviço; quem os dispara é o orquestrador,
não um operador. Por HTTP ficam só a consulta de saldo e a entrada de estoque, que é ação
humana e tem dono conhecido.

| Tópico | Papel deste serviço | Dono do contrato |
|---|---|---|
| `servicetrack.estoque.comandos.v1` | consumidor | `service-track-ordens`, o orquestrador |
| `servicetrack.estoque.eventos.v1` | produtor | **este serviço** |
| `servicetrack.estoque.comandos.v1.dlt` | destino de mensagem que não pode ser processada | este serviço |

Esquemas: [`esquemas/comandos-de-estoque-v1.json`](esquemas/comandos-de-estoque-v1.json) e
[`esquemas/eventos-de-estoque-v1.json`](esquemas/eventos-de-estoque-v1.json). Os dois são
copiados para o classpath de teste e validados contra o que o serviço produz e aceita — ver a
seção de teste de contrato mais abaixo.

> O esquema de **comandos** está aqui como contrato de leitura, não como propriedade. O dono é
> o `service-track-ordens`, que os publica e mantém o original em `docs/contratos/estoque/`.
> Este arquivo é cópia idêntica, com o risco de divergência que toda cópia tem — e é por isso
> que existe `ContratoDeComandosDeEstoqueTest`, que valida a cópia contra o que o consumidor
> realmente aceita. Decisão: `GLOBAL-ADR-010`.

## Envelope

Todo comando e todo evento têm a mesma casca. O corpo específico vive em `dados`.

```json
{
  "idMensagem": "9f1c7a02-5f8e-4e2a-9a71-0c3d5b6e8f10",
  "tipo": "ReservarEstoque",
  "versao": 1,
  "ocorridoEm": "2026-09-27T14:05:00Z",
  "correlationId": "atendimento-88213",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "dados": { }
}
```

| Campo | Para que serve |
|---|---|
| `idMensagem` | **chave de idempotência**. É o que impede o efeito duplicado quando a mesma mensagem é entregue duas vezes. Teto de 94 caracteres, porque o INBOX guarda `<tipo>:<idMensagem>` em 120 |
| `tipo` | qual comando ou evento é. Tipo desconhecido vai para a DLT, sem retentativa |
| `versao` | versão do contrato. Campo novo **não** muda a versão; remoção ou mudança de tipo muda |
| `ocorridoEm` | instante do fato, em UTC com offset |
| `correlationId` | amarra a operação de ponta a ponta nos logs. Ausente, o serviço gera um |
| `traceId` | redundância de depuração. O trace canônico é o cabeçalho `traceparent`, e é ele que vence quando os dois existem |

`dados` aceita campo desconhecido sem falhar — é o que torna a evolução aditiva possível sem
combinar deploy entre serviços.

## Chave da mensagem

`ordemServicoId`, sempre. Mesma ordem, mesma partição, ordem de entrega preservada: a saga
depende de `ReservarEstoque` chegar antes de `ConsumirReserva`. Por isso a tabela `OUTBOX`
tem `CHAVE_PARTICAO` própria — o agregado é o saldo do insumo, mas quem ordena é a ordem
de serviço.

## Comandos consumidos

| Tipo | Efeito | Evento publicado |
|---|---|---|
| `ReservarEstoque` | move do disponível para o reservado | `EstoqueReservado` ou `ReservaRecusada` |
| `ConsumirReserva` | baixa o reservado; não devolve nada | `EstoqueConsumido` |
| `LiberarReserva` | devolve o reservado ao disponível (compensação da saga) | `ReservaLiberada` |
| `RegistrarEntradaDeEstoque` | soma ao disponível | nenhum |

Saldo insuficiente **não é erro de processamento**: é resposta de negócio. A mensagem é
consumida com sucesso e a recusa vira `ReservaRecusada`, que é o que a saga precisa para
compensar. Tratar isso como exceção encheria a DLT de mensagens corretas.

## Eventos publicados

`EstoqueReservado`, `ReservaRecusada`, `EstoqueConsumido`, `ConsumoRecusado`, `ReservaLiberada`
e `ReservaExpirada`. O último não vem de comando: nasce da rotina que devolve ao estoque a
reserva cujo prazo venceu sem a saga concluir. Sem ela, uma saga abandonada prenderia peça
para sempre.

`ConsumoRecusado` **está no contrato e ainda não é produzido**. Existe porque consumir uma
reserva inexistente ou já expirada hoje vira violação de regra de domínio e vai para a DLT, e
uma saga cujo passo morre na DLT fica pendurada sem resposta. Implementar é etapa 4.

`expiraEm` sai **com offset**, em UTC. Até 07/10/2026 saía sem fuso nenhum, e a documentação
afirmava UTC enquanto o valor era a hora local da JVM.

Cabeçalhos de cada evento publicado:

| Cabeçalho | Conteúdo |
|---|---|
| `X-Tipo-Evento` | o mesmo valor de `tipo`, para o consumidor rotear sem desserializar |
| `X-Versao-Evento` | versão do contrato |
| `X-Trace-Id` | trace da operação que originou o evento. **Redundante** com o `traceparent`, que o Spring Kafka injeta sozinho; mantido na versão 1 porque remover cabeçalho é quebra de contrato, e nenhum consumidor deve depender dele |
| `traceparent` | W3C Trace Context, injetado pela observação do produtor. É por ele que o trace atravessa a fila |
| `X-Correlation-Id` | correlação da operação que originou o evento |

## Por que INBOX e OUTBOX

Toda entrega é **ao menos uma vez**, e gravar no banco e publicar na fila são duas operações
que falham em separado. As duas tabelas resolvem as duas metades:

- **INBOX** guarda o `idMensagem` já processado. A segunda entrega encontra a chave, não aplica
  nada e confirma o offset. A gravação do saldo, do movimento, da linha do INBOX e do evento
  acontece **numa transação só** — ou tudo, ou nada.
- **OUTBOX** recebe o evento na mesma transação do dado. Um publicador periódico lê o que está
  pendente com `FOR UPDATE SKIP LOCKED`, envia ao broker e só então marca a publicação. Broker
  fora do ar não perde evento: a linha continua pendente e sai na próxima passagem.

## Quando o broker não está disponível

O serviço **sobe e atende HTTP** sem broker. Verificado: `/actuator/health/readiness`
continua `UP`, consulta e entrada de estoque funcionam, o consumidor reconecta sozinho quando
o broker volta e o publicador drena o que ficou pendente.

Duas decisões sustentam isso:

1. A saúde do consumidor **não entra** no readiness. Se entrasse, uma indisponibilidade do
   broker tiraria do ar a parte HTTP, que não depende dele.
2. `missingTopicsFatal` é `false`. Em ambiente recém-criado o tópico pode ainda não existir;
   isso não é motivo para a aplicação não subir.

Falha do publicador é registrada uma vez em `ERROR`, depois esparsamente em `WARN`, e a
recuperação é anunciada em `INFO`. Sem isso, uma queda de dez minutos renderia centenas de
linhas iguais.

## Retentativa e DLT

| Situação | Comportamento |
|---|---|
| Falha transitória (banco indisponível, timeout) | até 4 tentativas com espera exponencial de 500 ms a 5 s |
| `MensagemInvalidaException` (JSON inválido, envelope incompleto, `dados` fora do contrato, tipo desconhecido) | **nenhuma** retentativa, vai direto para a DLT |
| Violação de regra de domínio (consumir reserva que não existe) | nenhuma retentativa, vai para a DLT |

O critério é simples: retentar só o que pode dar certo na segunda vez. Mensagem malformada
não melhora com espera — ocupa o consumidor e atrasa as mensagens boas atrás dela.

## Configuração

| Variável | Padrão | Efeito |
|---|---|---|
| `SERVICETRACK_MENSAGERIA_HABILITADA` | `false` | liga consumidor, publicador do outbox e rotina de expiração |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | endereço do broker |
| `SERVICETRACK_MENSAGERIA_GRUPO` | `catalogo-estoque` | grupo de consumo |
| `SERVICETRACK_TOPICO_COMANDOS` | `servicetrack.estoque.comandos.v1` | tópico de entrada |
| `SERVICETRACK_TOPICO_EVENTOS` | `servicetrack.estoque.eventos.v1` | tópico de saída |
| `SERVICETRACK_MENSAGERIA_CONCORRENCIA` | `1` | consumidores por instância |
| `SERVICETRACK_EXPIRACAO_RESERVAS` | `true` | rotina que devolve reserva vencida |

Desligada, nenhum bean de mensageria é criado — nem consumidor, nem publicador, nem agenda.
É o que permite rodar os testes e subir o serviço num ambiente sem broker.

## Como exercitar na mão

Com o serviço em pé e um broker acessível:

```bash
INSUMO=$(curl -s localhost:8080/insumos | python3 -c 'import json,sys; print(json.load(sys.stdin)[0]["id"])')
ORDEM=$(uuidgen | tr 'A-Z' 'a-z')

PRAZO=$(date -u -v+10M +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -d '+10 minutes' +%Y-%m-%dT%H:%M:%SZ)

printf '%s:{"idMensagem":"%s:RESERVA_DE_INSUMOS:%s","tipo":"ReservarEstoque","versao":1,"ocorridoEm":"2026-09-27T14:05:00Z","correlationId":"demo","dados":{"insumoId":"%s","ordemServicoId":"%s","quantidade":2,"expiraEm":"%s"}}\n' \
  "$ORDEM" "$ORDEM" "$INSUMO" "$INSUMO" "$ORDEM" "$PRAZO" \
  | kafka-console-producer.sh --bootstrap-server localhost:9092 \
      --topic servicetrack.estoque.comandos.v1 \
      --property parse.key=true --property key.separator=:

curl -s "localhost:8080/insumos/$INSUMO/estoque"
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic servicetrack.estoque.eventos.v1 --from-beginning --property print.headers=true
```

Reservar sem estoque suficiente devolve `ReservaRecusada` no tópico de eventos, não erro.
Repetir a mesma mensagem não reserva de novo — o log diz `mensagem ja processada`.

## Teste de contrato

`ContratoDeEventosDeEstoqueTest` e `ContratoDeComandosDeEstoqueTest`, 17 testes. O esquema não é
documentação que envelhece em silêncio: ele entra no classpath de teste a partir de
`docs/mensageria/esquemas` e é executado.

| O que o teste verifica | Por que importa |
|---|---|
| cada evento que a fábrica produz confere com o esquema | é o lado que outro serviço consome, e quebra de evento não dá erro HTTP |
| cada comando do esquema é lido pelo consumidor | contrato que o consumidor rejeita não é contrato |
| um evento mutilado é recusado | prova que a validação roda; esquema carregado errado aceita tudo em silêncio |
| a chave da saga cabe em `INBOX.ID`, lido por reflexão da entidade | é o orçamento que atravessa dois repositórios e que nada mais valida |
| campo desconhecido em `dados` passa | é o que torna a evolução aditiva possível sem deploy combinado |
| `traceparent` do cabeçalho vence o `traceId` do envelope | define qual dos dois é o canônico, e eles divergem |

O esquema aceita **menos** do que o consumidor, de propósito, em um ponto: `expiraEm` é
obrigatório no contrato e opcional no código. Apertar o contrato sem apertar o consumidor é o
que permite ao orquestrador ser corrigido sem combinar deploy com este serviço.
