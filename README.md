# service-track-catalogo

Microsserviço de **catálogo e estoque** da plataforma ServiceTrack.

É dono do que a oficina oferece e do que ela consome: serviços prestados, insumos com atributos
declarados em tempo de execução, e o saldo de estoque com reserva, consumo, liberação e
expiração. Não conhece usuário, não conhece ordem de serviço e não lê o banco de nenhum outro
serviço.

Arquitetura hexagonal: `domain` → `application` (portas e handlers) → `infrastructure`
(adaptadores de entrada e saída). O domínio não conhece Spring, JPA nem Kafka.

**Dois bancos, por desenho.** Postgres para o que tem transação e invariante numérico; MongoDB
para o insumo, cujo conjunto de atributos é definido pelo operador em tempo de execução. A
justificativa está em `GLOBAL-RFC-012`.

---

## O que este serviço expõe

Todas as rotas exigem o cabeçalho **`X-API-Version: 1`**. A versão é negociada por cabeçalho,
não por caminho — rota sem o cabeçalho não casa com nada e responde `404`.

| Método | Rota | Para quê |
|---|---|---|
| `GET` | `/categorias?termo=&incluirDesativadas=` | lista categorias de insumo |
| `POST` | `/categorias` | cria categoria, já com as definições de atributo |
| `GET` | `/categorias/{id}` | busca por identificador |
| `POST` | `/categorias/{id}/atributos` | acrescenta definição de atributo |
| `PUT` | `/categorias/{id}/ativacao` | desativa ou reativa |
| `GET` | `/insumos?termo=&categoriaId=&incluirInativos=` | lista insumos |
| `POST` | `/insumos` | cadastra insumo preenchendo os atributos da categoria |
| `GET` | `/insumos/{id}` | busca por identificador |
| `GET` | `/insumos/sku/{sku}` | busca por SKU |
| `PUT` | `/insumos/{id}` | altera dados do insumo |
| `DELETE` | `/insumos/{id}` | desativa |
| `GET` | `/insumos/{id}/estoque` | saldo: disponível, reservado e reservas ativas |
| `POST` | `/insumos/{id}/estoque/entradas` | registra entrada de estoque |
| `GET` | `/servicos?incluirInativos=` | lista serviços da oficina |
| `POST` | `/servicos` | cadastra serviço |
| `GET` | `/servicos/{id}` | busca por identificador |
| `PUT` | `/servicos/{id}` | altera serviço |
| `DELETE` | `/servicos/{id}` | desativa |

Documentação navegável em `/swagger-ui.html`, contrato em `/v3/api-docs`. Coleção pronta em
[docs/postman/](docs/postman/), com variáveis e geração de valores únicos por envio.

**Reserva, consumo e liberação de estoque não têm rota HTTP.** São comandos de mensageria,
porque são etapas de saga disparadas pelo orquestrador de ordem de serviço, não ações de
usuário. Ver a seção de mensageria.

### Regras de domínio que o serviço garante

- **Atributo é declarado na categoria e preenchido no insumo.** A categoria define nome, rótulo,
  tipo e se é obrigatório; o insumo responde. Atributo fora da definição é recusado.
- **SKU e código de barras são únicos** entre os insumos ativos.
- **Saldo nunca fica negativo.** Reserva acima do disponível é recusada com evento, não com
  exceção: quem pediu é uma saga, e saga precisa de resposta, não de erro de transporte.
- **Reserva pertence a uma ordem de serviço.** Consumo e liberação agem sobre a reserva daquela
  ordem, não sobre o saldo solto.
- **Reserva expira.** Uma rotina devolve ao disponível o que passou do prazo, com evento
  próprio. Desligável por `SERVICETRACK_EXPIRACAO_RESERVAS`.
- **Movimento de estoque é registrado em razão append-only** (`ESTOQUE_MOVIMENTOS`), com origem
  e chave de idempotência — o saldo é derivável do histórico.
- **Entrada é por lote**, com código, custo unitário e validade opcional. Código de lote é único
  por insumo, e a quantidade do lote nunca fica negativa.
- **Desativação é lógica.** Nada é apagado.
- **Insumo tem versão otimista.** Escrita concorrente no mesmo documento falha em vez de
  sobrescrever.

---

## Mensageria

Desligada por padrão (`SERVICETRACK_MENSAGERIA_HABILITADA=false`). Ligada, o serviço consome
comandos de estoque e publica eventos do que aconteceu.

| Tópico | Papel |
|---|---|
| `servicetrack.estoque.comandos.v1` | entrada: `ReservarEstoque`, `ConsumirReserva`, `LiberarReserva`, `RegistrarEntradaDeEstoque` |
| `servicetrack.estoque.eventos.v1` | saída: `EstoqueReservado`, `EstoqueConsumido`, `ReservaLiberada`, `ReservaRecusada`, `ReservaExpirada` |
| `<tópico>.dlt` | mensagem que falhou depois de 4 tentativas com espera exponencial de 500ms a 5s |

O contrato de cada tópico está em [docs/mensageria/esquemas/](docs/mensageria/esquemas/), em
JSON Schema. **O dono do contrato de comandos é quem publica**; o arquivo aqui é a leitura que
este serviço faz dele. Evolução é aditiva: campo desconhecido em `dados` é ignorado, para que
mudança não exija deploy combinado.

Garantias:

- **Consumidor idempotente.** O `idMensagem` é a chave, e a identidade no `INBOX` é
  `tipo:chave` — chave repetida no mesmo tipo de comando não aplica efeito duas vezes, e a mesma
  chave em comandos diferentes continua valendo. Todo broker entrega ao menos uma vez.
- **Publicação por OUTBOX.** O evento é gravado na mesma transação do efeito e publicado depois
  por uma rotina, então não existe efeito aplicado sem evento nem evento sem efeito.
- **Mensagem fora do contrato não é retentada.** Vai direto para a DLT, porque retentar corpo
  inválido só gasta tempo.

> **O trace ainda não atravessa a fila.** O `traceId` viaja como campo do envelope e é copiado
> para os eventos de saída, mas nenhum `traceparent` vai no cabeçalho da mensagem, então a
> ferramenta de rastreio vê dois traces desconexos. `spring.kafka.template.observation-enabled`
> já está ligado e foi medido como insuficiente. Detalhe e medição em `GLOBAL-ADR-007`.

Visão completa em [docs/mensageria/](docs/mensageria/).

---

## Rodar local

Tudo com Docker, a partir da raiz do repositório.

```bash
docker compose up -d --build
```

Sobe Postgres, MongoDB, Kafka em KRaft, a criação dos tópicos, a aplicação e a pilha de
observabilidade. A aplicação responde em `http://localhost:8080`.

| Serviço | Endereço |
|---|---|
| Aplicação | `http://localhost:8080` |
| Swagger | `http://localhost:8080/swagger-ui.html` |
| Grafana | `http://localhost:3000` |
| Prometheus | `http://localhost:9090` |

Os bancos nascem com baseline e seed aplicados de `db/`, pelo diretório de inicialização de cada
imagem. Passo a passo e solução de problemas em [docs/ambiente-local.md](docs/ambiente-local.md).

**A chave de validação do JWT fica em `keys/`, que não é versionado** (`.gitignore`). Para rodar
local sem autenticação, use o perfil de teste; para rodar com, gere um par e aponte
`SERVICETRACK_JWT_CHAVE_PUBLICA`.

### Testes

```bash
cd software && ./gradlew test
```

Os testes com sufixo `IT` usam Testcontainers e exigem Docker. Os demais rodam sem nada
externo: o perfil `teste` exclui a autoconfiguração de banco e usa adaptadores em memória.

---

## Configuração

Nada tem valor fixo de ambiente no código. O que não tem padrão é obrigatório.

| Variável | Padrão | Para quê |
|---|---|---|
| `ST_CAT_DB_URL` | — | JDBC do Postgres `st_cat` |
| `ST_CAT_DB_USER` | — | usuário do Postgres |
| `ST_CAT_DB_PASSWORD` | — | senha do Postgres |
| `ST_CAT_DB_POOL_MAX` | `10` | teto do pool, declarado no orçamento de conexão |
| `ST_CAT_DB_POOL_MIN` | `2` | mínimo do pool |
| `ST_INS_MONGO_URI` | — | URI do MongoDB `ST_INS` |
| `SERVICETRACK_JWT_CHAVE_PUBLICA` | `file:/keys/publicKey.pem` | chave pública RS256 que valida o token |
| `SERVICETRACK_MENSAGERIA_HABILITADA` | `false` | liga consumidor, publicador e expiração |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | broker |
| `SERVICETRACK_MENSAGERIA_GRUPO` | `catalogo-estoque` | grupo de consumo |
| `SERVICETRACK_TOPICO_COMANDOS` | `servicetrack.estoque.comandos.v1` | tópico de entrada |
| `SERVICETRACK_TOPICO_EVENTOS` | `servicetrack.estoque.eventos.v1` | tópico de saída |
| `SERVICETRACK_MENSAGERIA_CONCORRENCIA` | `1` | consumidores em paralelo |
| `SERVICETRACK_EXPIRACAO_RESERVAS` | `true` | liga a rotina de expiração |

**Credencial de banco nunca vem de ConfigMap nem do Terraform.** Em `hml` e `prd` ela vem de um
`Secret` montado a partir do SSM pela esteira (`CAT-ADR-001`).

### Schema dos bancos

Não há Flyway nem migração automática: `ddl-auto` é `validate`, e o baseline em `db/` **é** o
schema.

| Arquivo | O que faz |
|---|---|
| `db/postgres/01_baseline_st_cat.sql` | schema `CATALOGO`: `SERVICOS`, `ESTOQUE_SALDOS`, `ESTOQUE_LOTES`, `ESTOQUE_RESERVAS`, `ESTOQUE_MOVIMENTOS`, `INBOX`, `OUTBOX` |
| `db/postgres/02_seed_servicos.sql` | serviços de exemplo |
| `db/mongo/01_baseline_st_ins.js` | coleções e índices de `CATEGORIAS` e `INSUMOS` |
| `db/mongo/02_seed_st_ins.js` | categorias e insumos de exemplo |

Como os ambientes são efêmeros e o baseline é sempre reaplicado do zero, **editar o seed é o
caminho normal**, não uma exceção. Em `hml` e `prd` quem aplica é `scripts/aplicar-baseline.sh`,
pela esteira Banco.

---

## Observabilidade

### Padrão de log

Toda linha carrega, entre colchetes, a aplicação e cinco campos de correlação
(`GLOBAL-ADR-006`, `CAT-ADR-003`):

```
[service-track-catalogo,<traceId>,<spanId>,<correlationId>,<requestId>]
```

- `traceId` e `spanId` vêm do tracing e amarram a requisição a toda a árvore de chamadas.
- `correlationId` atravessa serviços. Vem do cabeçalho `X-Correlation-Id` quando o cliente
  manda, e é gerado quando não manda. Volta na resposta.
- `requestId` é único por requisição, sempre gerado aqui.

No consumo de mensagem a correlação vem do envelope e o `requestId` é novo, porque um comando
reentregue é outra execução. O `idMensagem` aparece como campo próprio do log.

`GET` que termina em 2xx loga em `DEBUG`, de propósito — em `INFO` ficam apenas escrita, erro de
cliente (`WARN`) e erro de servidor (`ERROR`). Rotas de `/actuator`, `/v3/api-docs` e
`/swagger-ui` não geram linha de acesso.

Em `hml` e `prd` o formato é JSON estruturado; local é texto legível.

### Métricas e painéis

Métricas em `/actuator/prometheus`, incluindo as de caso de uso e de estoque, além das de JVM,
HTTP e Kafka. A pilha local traz Prometheus, Loki, Tempo e Grafana já provisionados, com
painéis e alertas versionados em `observabilidade/`.

Detalhe do que é medido e de como ler cada painel em
[docs/observabilidade.md](docs/observabilidade.md).

---

## Testes e cobertura

100 testes de unidade e de componente, mais integração com Testcontainers para os adaptadores de
Postgres. O CI publica o relatório de testes como artefato a cada execução.

> **Lacuna conhecida:** não existe portão de cobertura neste repositório. A Fase 4 exige **80%
> por serviço, com evidência publicada no README**, e isto ainda não está medido aqui. O
> `service-track-usuarios-veiculos` já tem o portão com JaCoCo; replicar o mesmo desenho aqui é
> trabalho pendente, não decisão em aberto.

O CI tem uma **guarda de segredos** que roda antes de compilar: falha se alguma chave privada
estiver rastreada, se um PEM versionado contiver bloco privado, ou se ferramental local tiver
vazado para o repositório.

---

## Como este serviço chega em hml e prd

Quatro esteiras, nesta ordem, todas acionadas à mão:

| # | Esteira | O que faz |
|---|---|---|
| 1 | **Infra** | `infra/terraform`: ECR, RDS `st_cat`, parâmetros no SSM. Chama a Banco ao terminar |
| 2 | **Banco** | cria o `Secret` a partir do SSM e aplica o baseline |
| 3 | **CI** | guarda de segredos, build, testes e imagem |
| 4 | **CD** | publica a imagem no ECR e abre PR escrevendo `newName` e `newTag` no overlay |

O merge do PR é o portão de deploy: o ArgoCD sincroniza `k8s/overlays/<ambiente>` a partir da
`main`. **O registro da imagem não está escrito no repositório** — a URL do ECR carrega o
identificador da conta AWS, que muda a cada laboratório, então quem escreve é a esteira
(`CAT-ADR-002`).

A plataforma (rede, EKS, ArgoCD) vive em `service-track-aws-iac` e precisa estar de pé antes.
Manifestos e dependências de runtime em [k8s/README.md](k8s/README.md).

---

## Convenção de nomes

**Domínio e aplicação em português. Ponto de encaixe com o framework fica na linguagem do
framework.** O radical nomeia o assunto, o sufixo nomeia o papel técnico.

| Papel | Sufixo | Exemplo |
|---|---|---|
| `@Configuration` | `Config` | `MensageriaConfig`, `SecurityConfig`, `OpenApiConfig` |
| `@ConfigurationProperties` | `Properties` | `MensageriaProperties`, `JwtProperties` |
| Caso de uso de escrita | `CommandHandler` | `EstoqueCommandHandler` |
| Caso de uso de leitura | `QueryHandler` | `InsumoQueryHandler` |
| Filtro de servlet | `Filter` | `CorrelacaoFilter` |
| `@RestControllerAdvice` | `ExceptionHandler` | `GlobalExceptionHandler` |
| Exceção de domínio | `Exception` | `SaldoInsuficienteException` |
| Mapeador | `Mapper` | `InsumoWebMapper`, `InsumoPersistenceMapper` |

**Método `@Bean` se chama como o tipo que devolve**, em camelCase: `openApi`, `jwtDecoder`,
`deadLetterPublishingRecoverer`. Duas exceções, deliberadas:

- **Mais de um bean do mesmo tipo** exige nomes distintos, e aí o nome descreve o papel —
  `exigirTokenNasOperacoes` e `documentarCorrelacao`, ambos `OperationCustomizer`.
- **`estoqueListenerContainerFactory`** não se chama `kafkaListenerContainerFactory` de
  propósito: esse é o nome padrão do Spring Boot, e assumi-lo faria esta fábrica substituir a
  auto-configurada.

O que **não** muda de língua: domínio, casos de uso, regras, e o que é conceito nosso e não do
framework — `AgendaDeMensageria`, `ExpiracaoDeReservas`, `FiltroDeInsumo` (critério de consulta,
não filtro de servlet), `MetricasDeEstoque`.

---

## Fronteiras

**É dono de:** serviços da oficina, categorias e insumos, saldo e reservas de estoque, o contrato
dos eventos de estoque, o próprio ECR, o próprio RDS e o próprio MongoDB.

**Não é dono e não altera:** usuário, veículo, ordem de serviço, pagamento, plataforma AWS,
API Gateway, e o banco de qualquer outro serviço.

Precisou de dado alheio: chama a API do dono ou consome um evento dele. **Nunca o banco.**

---

## Decisões

| ADR | Assunto |
|---|---|
| [CAT-ADR-001](docs/adr/CAT-ADR-001-credencial-do-banco-por-secret-da-esteira.md) | credencial do banco vem de `Secret` criado pela esteira |
| [CAT-ADR-002](docs/adr/CAT-ADR-002-registro-da-imagem-escrito-pela-esteira.md) | registro da imagem escrito pela esteira, não versionado |
| [CAT-ADR-003](docs/adr/CAT-ADR-003-alinhamento-do-padrao-de-log.md) | alinhamento ao padrão de log dos microsserviços |
| [CAT-ADR-004](docs/adr/CAT-ADR-004-dimensionamento-de-memoria-e-hpa.md) | teto de heap abaixo do limite do contêiner, e HPA só por CPU |

O `CAT-ADR-004` chega pelo PR de dimensionamento de memória; até ele entrar na `main`, o link
acima fica pendente.

Decisões que atravessam repositórios usam o prefixo `GLOBAL-` e vivem fora deste repositório.
