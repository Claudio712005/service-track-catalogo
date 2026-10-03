# CAT-ADR-003: alinhamento ao padrão de log dos microsserviços

## Data
03/10/2026

## Status
Aceita. Implementa o `GLOBAL-ADR-006` neste serviço.

---

## Contexto

Este serviço nasceu antes do padrão comum e divergia dele em quatro pontos, todos verificados no
código e em log colhido de homologação:

1. **Nome do identificador da requisição.** Aqui era `transactionId`, com cabeçalho de resposta
   `X-Transaction-Id`; no `usuarios-veiculos`, `requestId`. Consulta que cruza serviços precisaria de
   `or` em todo painel e alerta.
2. **O campo de rastro imprimia vazio em toda linha.** O padrão lia `%X{trace_id}`, e essa chave não
   existe nesta pilha: o que vai para o MDC é `traceId` e `spanId`, em caixa camelo. Medido no serviço
   irmão, com as duas convenções lado a lado.
3. **Amostragem.** Sem `management.tracing`, valia o padrão do Spring Boot, `0.1`: nove de cada dez
   requisições sem span, mesmo com a chave certa.
4. **Ordem do filtro.** Em `HIGHEST_PRECEDENCE`, o filtro envolve o filtro de observação do Spring e o
   log de acesso sai do `finally` com o escopo do span já fechado — sem rastro na linha que mais
   importa.

## Decisão

- `transactionId` passa a ser **`requestId`**, e o cabeçalho de resposta `X-Transaction-Id` passa a ser
  **`X-Request-Id`**.
- O padrão de log passa a `[serviço, traceId, spanId, correlationId, requestId]`.
- `management.tracing.sampling.probability: 1.0`, e o exportador OTLP desligado enquanto não houver
  coletor — ligado sem coletor, o serviço registrava WARN com pilha a cada dez segundos tentando
  `localhost:4318`.
- O filtro vai para `Ordered.HIGHEST_PRECEDENCE + 10`.
- O log de acesso passa a registrar a **rota em template** (`/insumos/{id}`), lida de
  `HandlerMapping.bestMatchingPattern`, em vez do caminho com identificador.
- **No consumidor de mensagem**, `requestId` passa a ser gerado por tentativa de processamento, e o
  `idMensagem` vira campo próprio na linha de log. Antes o `idMensagem` ocupava o lugar do
  identificador da requisição; com entrega ao menos uma vez, duas tentativas da mesma mensagem ficavam
  indistinguíveis — e é justamente isso que esse campo existe para separar. O `idMensagem` continua
  visível, agora como o que ele é.

## Consequências

- **Quebra de contrato observável, aceita sem versão nova.** O cabeçalho `X-Transaction-Id` desaparece.
  Nenhum cliente nosso o lê: a coleção do Postman não o usa, e o `app-ios` está fora de escopo.
- O texto das linhas de log muda: rota em template e nome de campo novo. Nenhum painel atual depende
  do texto cru.
- O ruído do exportador OTLP sai do log.
- A travessia da fila fica **meio caminho andada**: o `LeitorDeEnvelope` já lê `traceparent` e
  `X-Correlation-Id` da mensagem, e o consumidor reidrata o MDC. Falta o lado do publicador escrever
  esses cabeçalhos quando este serviço passar a publicar.
- O efeito no rastro **não foi medido neste serviço**: subir o catálogo exige Postgres, Mongo e Kafka.
  O mecanismo foi provado no `usuarios-veiculos`, que roda a mesma versão do Boot e o mesmo starter, e
  as três mudanças aqui são as mesmas. A primeira subida em homologação confirma.
