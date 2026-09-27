CREATE SCHEMA IF NOT EXISTS CATALOGO;

SET SEARCH_PATH TO CATALOGO;

CREATE TABLE IF NOT EXISTS SERVICOS (
    ID UUID NOT NULL,
    NOME_SERVICO VARCHAR(150) NOT NULL,
    DESCRICAO_SERVICO TEXT NOT NULL,
    VALOR_REFERENCIA NUMERIC(12,2),
    ATIVO BOOLEAN NOT NULL DEFAULT TRUE,
    DATA_CRIACAO TIMESTAMPTZ(6) NOT NULL,
    DATA_ATUALIZACAO TIMESTAMPTZ(6) NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT CK_SERVICOS_VALOR_REFERENCIA CHECK (VALOR_REFERENCIA IS NULL OR VALOR_REFERENCIA >= 0)
);

COMMENT ON TABLE SERVICOS IS 'Servicos de mao de obra que a oficina executa e cobra. Nao confundir com insumo, que e material e vive no MongoDB.';
COMMENT ON COLUMN SERVICOS.ID IS 'Identificador gerado pela aplicacao, UUID v7, ordenado no tempo para nao fragmentar o indice da chave primaria.';
COMMENT ON COLUMN SERVICOS.NOME_SERVICO IS 'Nome comercial do servico, como aparece no orcamento ao cliente.';
COMMENT ON COLUMN SERVICOS.DESCRICAO_SERVICO IS 'Descricao tecnica do que o servico inclui, exibida na tela do mecanico.';
COMMENT ON COLUMN SERVICOS.VALOR_REFERENCIA IS 'Preco de tabela em reais. E referencia: o valor cobrado fica congelado no item da ordem de servico, em outro banco.';
COMMENT ON COLUMN SERVICOS.ATIVO IS 'Falso desativa o servico para novos orcamentos, sem apagar historico. Servico nunca e removido.';
COMMENT ON COLUMN SERVICOS.DATA_CRIACAO IS 'Instante do cadastro, em UTC.';
COMMENT ON COLUMN SERVICOS.DATA_ATUALIZACAO IS 'Instante da ultima alteracao, em UTC. Escrito pela aplicacao, nao por trigger.';

CREATE UNIQUE INDEX IF NOT EXISTS UQ_SERVICOS_NOME ON SERVICOS (LOWER(NOME_SERVICO));
CREATE INDEX IF NOT EXISTS IX_SERVICOS_ATIVOS ON SERVICOS (NOME_SERVICO) WHERE ATIVO;

COMMENT ON INDEX UQ_SERVICOS_NOME IS 'Impede dois servicos com o mesmo nome variando so caixa de digitacao.';
COMMENT ON INDEX IX_SERVICOS_ATIVOS IS 'A tela de orcamento lista apenas servico ativo, em ordem de nome. Indice parcial le so o que interessa.';

CREATE TABLE IF NOT EXISTS ESTOQUE_SALDOS (
    INSUMO_ID UUID NOT NULL,
    QUANTIDADE_DISPONIVEL NUMERIC(14,4) NOT NULL DEFAULT 0,
    QUANTIDADE_RESERVADA NUMERIC(14,4) NOT NULL DEFAULT 0,
    ESTOQUE_MINIMO NUMERIC(14,4) NOT NULL DEFAULT 0,
    UNIDADE_MEDIDA VARCHAR(20) NOT NULL,
    VERSAO INTEGER NOT NULL DEFAULT 0,
    DATA_ATUALIZACAO TIMESTAMPTZ(6) NOT NULL,
    PRIMARY KEY (INSUMO_ID),
    CONSTRAINT CK_SALDOS_DISPONIVEL CHECK (QUANTIDADE_DISPONIVEL >= 0),
    CONSTRAINT CK_SALDOS_RESERVADA CHECK (QUANTIDADE_RESERVADA >= 0),
    CONSTRAINT CK_SALDOS_MINIMO CHECK (ESTOQUE_MINIMO >= 0)
);

COMMENT ON TABLE ESTOQUE_SALDOS IS 'Posicao atual de cada insumo. E projecao transacional de ESTOQUE_MOVIMENTOS: reconstruivel somando o razao, existe para leitura rapida e para o CHECK recusar saldo negativo.';
COMMENT ON COLUMN ESTOQUE_SALDOS.INSUMO_ID IS 'Referencia logica ao documento do insumo no MongoDB ST_INS. NAO ha FOREIGN KEY: o dono do dado esta em outro engine. Saldo orfao e detectado por rotina, nao pelo banco.';
COMMENT ON COLUMN ESTOQUE_SALDOS.QUANTIDADE_DISPONIVEL IS 'Quantidade livre para reservar. Ja desconta o reservado.';
COMMENT ON COLUMN ESTOQUE_SALDOS.QUANTIDADE_RESERVADA IS 'Soma das reservas ativas. Invariante: igual a soma de ESTOQUE_RESERVAS com STATUS ATIVA.';
COMMENT ON COLUMN ESTOQUE_SALDOS.ESTOQUE_MINIMO IS 'Limite de reposicao. Mora aqui, e nao no cadastro, porque politica de estoque e do estoque e porque o indice parcial de alerta compara duas colunas da mesma tabela.';
COMMENT ON COLUMN ESTOQUE_SALDOS.UNIDADE_MEDIDA IS 'Copia da unidade vigente no primeiro movimento. Congelada: trocar a unidade depois mudaria o significado dos numeros ja gravados.';
COMMENT ON COLUMN ESTOQUE_SALDOS.VERSAO IS 'Contador de bloqueio otimista. A atualizacao usa WHERE VERSAO = valor lido; quem perder a corrida tenta de novo.';
COMMENT ON COLUMN ESTOQUE_SALDOS.DATA_ATUALIZACAO IS 'Instante da ultima mudanca de saldo, em UTC.';

CREATE INDEX IF NOT EXISTS IX_SALDOS_ABAIXO_DO_MINIMO ON ESTOQUE_SALDOS (INSUMO_ID)
    WHERE QUANTIDADE_DISPONIVEL < ESTOQUE_MINIMO;

COMMENT ON INDEX IX_SALDOS_ABAIXO_DO_MINIMO IS 'Alerta de reposicao: le apenas as linhas ja em falta, em vez de varrer a tabela comparando coluna com coluna.';

CREATE TABLE IF NOT EXISTS ESTOQUE_LOTES (
    ID UUID NOT NULL,
    INSUMO_ID UUID NOT NULL,
    CODIGO_LOTE VARCHAR(60) NOT NULL,
    VALIDADE DATE,
    CUSTO_UNITARIO NUMERIC(12,2) NOT NULL,
    QUANTIDADE_ATUAL NUMERIC(14,4) NOT NULL DEFAULT 0,
    DATA_RECEBIMENTO TIMESTAMPTZ(6) NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT UQ_LOTES_INSUMO_CODIGO UNIQUE (INSUMO_ID, CODIGO_LOTE),
    CONSTRAINT CK_LOTES_QUANTIDADE CHECK (QUANTIDADE_ATUAL >= 0),
    CONSTRAINT CK_LOTES_CUSTO CHECK (CUSTO_UNITARIO >= 0)
);

COMMENT ON TABLE ESTOQUE_LOTES IS 'Lotes recebidos de um insumo, para consumo por validade e rastreabilidade de recall. So existe para insumo cujo cadastro marca CONTROLA_LOTE.';
COMMENT ON COLUMN ESTOQUE_LOTES.CODIGO_LOTE IS 'Codigo impresso na embalagem pelo fabricante. Unico por insumo.';
COMMENT ON COLUMN ESTOQUE_LOTES.VALIDADE IS 'Data de vencimento. Nulo para insumo que nao vence.';
COMMENT ON COLUMN ESTOQUE_LOTES.CUSTO_UNITARIO IS 'Custo pago neste lote, usado para valorar a saida. Difere do custo de tabela do cadastro.';
COMMENT ON COLUMN ESTOQUE_LOTES.QUANTIDADE_ATUAL IS 'Saldo remanescente do lote. A soma dos lotes de um insumo bate com o saldo total quando o insumo controla lote.';
COMMENT ON COLUMN ESTOQUE_LOTES.DATA_RECEBIMENTO IS 'Instante da entrada do lote, em UTC.';

CREATE INDEX IF NOT EXISTS IX_LOTES_CONSUMO ON ESTOQUE_LOTES (INSUMO_ID, VALIDADE NULLS LAST)
    WHERE QUANTIDADE_ATUAL > 0;

COMMENT ON INDEX IX_LOTES_CONSUMO IS 'Consumo pelo lote que vence primeiro: ordena por validade dentro do insumo, ignorando lote zerado.';

CREATE TABLE IF NOT EXISTS ESTOQUE_RESERVAS (
    ID UUID NOT NULL,
    INSUMO_ID UUID NOT NULL,
    ORDEM_SERVICO_ID UUID NOT NULL,
    QUANTIDADE NUMERIC(14,4) NOT NULL,
    STATUS VARCHAR(20) NOT NULL,
    EXPIRA_EM TIMESTAMPTZ(6),
    DATA_CRIACAO TIMESTAMPTZ(6) NOT NULL,
    DATA_ENCERRAMENTO TIMESTAMPTZ(6),
    PRIMARY KEY (ID),
    CONSTRAINT CK_RESERVAS_QUANTIDADE CHECK (QUANTIDADE > 0),
    CONSTRAINT CK_RESERVAS_STATUS CHECK (STATUS IN ('ATIVA', 'CONSUMIDA', 'LIBERADA', 'EXPIRADA')),
    CONSTRAINT CK_RESERVAS_ENCERRAMENTO CHECK ((STATUS = 'ATIVA') = (DATA_ENCERRAMENTO IS NULL))
);

COMMENT ON TABLE ESTOQUE_RESERVAS IS 'Compromisso de material para uma ordem de servico. E o passo compensavel da saga: nasce ATIVA na aprovacao do orcamento e termina CONSUMIDA, LIBERADA ou EXPIRADA.';
COMMENT ON COLUMN ESTOQUE_RESERVAS.INSUMO_ID IS 'Referencia logica ao documento do insumo no MongoDB ST_INS.';
COMMENT ON COLUMN ESTOQUE_RESERVAS.ORDEM_SERVICO_ID IS 'Referencia logica a ordem no banco ST_OS, de outro servico. Sem FOREIGN KEY, por desenho.';
COMMENT ON COLUMN ESTOQUE_RESERVAS.QUANTIDADE IS 'Quantidade comprometida, na unidade do insumo.';
COMMENT ON COLUMN ESTOQUE_RESERVAS.STATUS IS 'ATIVA segura o saldo; CONSUMIDA virou baixa; LIBERADA foi compensada; EXPIRADA venceu sem desfecho e devolveu o saldo.';
COMMENT ON COLUMN ESTOQUE_RESERVAS.EXPIRA_EM IS 'Prazo para a saga concluir. Vencido, a rotina devolve o saldo. Rede de seguranca para saga que morreu no meio.';
COMMENT ON COLUMN ESTOQUE_RESERVAS.DATA_CRIACAO IS 'Instante da reserva, em UTC.';
COMMENT ON COLUMN ESTOQUE_RESERVAS.DATA_ENCERRAMENTO IS 'Instante em que saiu de ATIVA. Nulo enquanto ativa, obrigatorio depois, garantido por CHECK.';

CREATE UNIQUE INDEX IF NOT EXISTS UQ_RESERVAS_ATIVA_POR_ORDEM ON ESTOQUE_RESERVAS (ORDEM_SERVICO_ID, INSUMO_ID)
    WHERE STATUS = 'ATIVA';

CREATE INDEX IF NOT EXISTS IX_RESERVAS_INSUMO_STATUS ON ESTOQUE_RESERVAS (INSUMO_ID, STATUS);
CREATE INDEX IF NOT EXISTS IX_RESERVAS_A_EXPIRAR ON ESTOQUE_RESERVAS (EXPIRA_EM) WHERE STATUS = 'ATIVA';

COMMENT ON INDEX UQ_RESERVAS_ATIVA_POR_ORDEM IS 'Mensagem reentregue nao vira reserva dobrada: a segunda tentativa colide aqui. E controle de idempotencia, nao otimizacao.';
COMMENT ON INDEX IX_RESERVAS_INSUMO_STATUS IS 'Reservas de um insumo por situacao, usado na conferencia da invariante de saldo reservado.';
COMMENT ON INDEX IX_RESERVAS_A_EXPIRAR IS 'A rotina de expiracao varre so o que esta ativo e com prazo.';

CREATE TABLE IF NOT EXISTS ESTOQUE_MOVIMENTOS (
    ID UUID NOT NULL,
    INSUMO_ID UUID NOT NULL,
    LOTE_ID UUID,
    RESERVA_ID UUID,
    TIPO VARCHAR(20) NOT NULL,
    QUANTIDADE NUMERIC(14,4) NOT NULL,
    UNIDADE_MEDIDA VARCHAR(20) NOT NULL,
    CUSTO_UNITARIO NUMERIC(12,2),
    SALDO_DISPONIVEL_APOS NUMERIC(14,4) NOT NULL,
    ORIGEM_TIPO VARCHAR(30) NOT NULL,
    ORIGEM_ID UUID,
    CHAVE_IDEMPOTENCIA VARCHAR(120) NOT NULL,
    REGISTRADO_POR UUID,
    DATA_CRIACAO TIMESTAMPTZ(6) NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT FK_MOVIMENTOS_LOTE FOREIGN KEY (LOTE_ID) REFERENCES ESTOQUE_LOTES (ID),
    CONSTRAINT FK_MOVIMENTOS_RESERVA FOREIGN KEY (RESERVA_ID) REFERENCES ESTOQUE_RESERVAS (ID),
    CONSTRAINT UQ_MOVIMENTOS_IDEMPOTENCIA UNIQUE (CHAVE_IDEMPOTENCIA),
    CONSTRAINT CK_MOVIMENTOS_QUANTIDADE CHECK (QUANTIDADE > 0),
    CONSTRAINT CK_MOVIMENTOS_TIPO CHECK (TIPO IN (
        'ENTRADA', 'SAIDA', 'AJUSTE_POSITIVO', 'AJUSTE_NEGATIVO',
        'RESERVA', 'LIBERACAO_RESERVA', 'CONSUMO_RESERVA', 'DEVOLUCAO'))
);

COMMENT ON TABLE ESTOQUE_MOVIMENTOS IS 'Razao de estoque, APPEND-ONLY. E a fonte da verdade: ESTOQUE_SALDOS e derivado daqui. Correcao entra como movimento novo; linha existente nunca e alterada nem apagada.';
COMMENT ON COLUMN ESTOQUE_MOVIMENTOS.INSUMO_ID IS 'Referencia logica ao documento do insumo no MongoDB ST_INS.';
COMMENT ON COLUMN ESTOQUE_MOVIMENTOS.LOTE_ID IS 'Lote movimentado, quando o insumo controla lote.';
COMMENT ON COLUMN ESTOQUE_MOVIMENTOS.RESERVA_ID IS 'Reserva que originou o movimento, nos tipos RESERVA, CONSUMO_RESERVA e LIBERACAO_RESERVA.';
COMMENT ON COLUMN ESTOQUE_MOVIMENTOS.TIPO IS 'Natureza do movimento. RESERVA e LIBERACAO_RESERVA nao mudam quantidade fisica, mudam disponibilidade.';
COMMENT ON COLUMN ESTOQUE_MOVIMENTOS.QUANTIDADE IS 'Quantidade sempre positiva. O sinal esta no tipo, nao no numero.';
COMMENT ON COLUMN ESTOQUE_MOVIMENTOS.UNIDADE_MEDIDA IS 'Unidade vigente quando o movimento ocorreu. Copiada de proposito: o extrato de ontem nao muda porque o cadastro mudou hoje.';
COMMENT ON COLUMN ESTOQUE_MOVIMENTOS.CUSTO_UNITARIO IS 'Custo praticado no movimento. Nulo quando o movimento nao tem valor, como reserva.';
COMMENT ON COLUMN ESTOQUE_MOVIMENTOS.SALDO_DISPONIVEL_APOS IS 'Saldo resultante. Redundante com a soma do razao, e e isso que permite detectar divergencia entre saldo e razao.';
COMMENT ON COLUMN ESTOQUE_MOVIMENTOS.ORIGEM_TIPO IS 'Quem causou: ORDEM_SERVICO, NOTA_ENTRADA, INVENTARIO, AJUSTE_MANUAL.';
COMMENT ON COLUMN ESTOQUE_MOVIMENTOS.ORIGEM_ID IS 'Id no sistema de origem. Para ORDEM_SERVICO, aponta para ST_OS, sem FOREIGN KEY.';
COMMENT ON COLUMN ESTOQUE_MOVIMENTOS.CHAVE_IDEMPOTENCIA IS 'Derivada do evento, por exemplo OS:{ORDEM}:CONSUMO:{INSUMO}:V1. A unicidade e o mecanismo de idempotencia do consumidor de mensagem.';
COMMENT ON COLUMN ESTOQUE_MOVIMENTOS.REGISTRADO_POR IS 'Usuario responsavel, referencia logica a ST_IDT. Nulo quando a origem e automatica.';
COMMENT ON COLUMN ESTOQUE_MOVIMENTOS.DATA_CRIACAO IS 'Instante do movimento, em UTC.';

CREATE INDEX IF NOT EXISTS IX_MOVIMENTOS_INSUMO_DATA ON ESTOQUE_MOVIMENTOS (INSUMO_ID, DATA_CRIACAO DESC);
CREATE INDEX IF NOT EXISTS IX_MOVIMENTOS_ORIGEM ON ESTOQUE_MOVIMENTOS (ORIGEM_TIPO, ORIGEM_ID) WHERE ORIGEM_ID IS NOT NULL;

COMMENT ON INDEX IX_MOVIMENTOS_INSUMO_DATA IS 'Extrato do insumo e reconstrucao do saldo, sempre por insumo e periodo, do mais recente para o mais antigo.';
COMMENT ON INDEX IX_MOVIMENTOS_ORIGEM IS 'Responde "o que esta ordem consumiu", pergunta da auditoria e do estorno.';

CREATE TABLE IF NOT EXISTS OUTBOX (
    ID UUID NOT NULL,
    AGREGADO_TIPO VARCHAR(40) NOT NULL,
    AGREGADO_ID UUID NOT NULL,
    TIPO_EVENTO VARCHAR(60) NOT NULL,
    VERSAO_EVENTO SMALLINT NOT NULL DEFAULT 1,
    PAYLOAD JSONB NOT NULL,
    TRACE_ID VARCHAR(64),
    DATA_CRIACAO TIMESTAMPTZ(6) NOT NULL,
    DATA_PUBLICACAO TIMESTAMPTZ(6),
    PRIMARY KEY (ID)
);

CREATE TABLE IF NOT EXISTS INBOX (
    ID VARCHAR(120) NOT NULL,
    TIPO_EVENTO VARCHAR(60) NOT NULL,
    DATA_PROCESSAMENTO TIMESTAMPTZ(6) NOT NULL,
    PRIMARY KEY (ID)
);

COMMENT ON TABLE OUTBOX IS 'Evento gravado na MESMA transacao que muda o dado, publicado depois por um leitor. Sem isso, gravar no banco e publicar na fila sao dois passos que falham em separado.';
COMMENT ON COLUMN OUTBOX.AGREGADO_TIPO IS 'Agregado que originou o evento: INSUMO, SALDO_ESTOQUE, SERVICO.';
COMMENT ON COLUMN OUTBOX.TIPO_EVENTO IS 'Nome do evento publicado, por exemplo ESTOQUE_RESERVADO.';
COMMENT ON COLUMN OUTBOX.VERSAO_EVENTO IS 'Versao do contrato do evento. Evolucao e aditiva: campo novo nao muda a versao, remocao muda.';
COMMENT ON COLUMN OUTBOX.PAYLOAD IS 'Corpo do evento em JSONB, no formato do contrato publicado.';
COMMENT ON COLUMN OUTBOX.TRACE_ID IS 'Identificador do trace, que viaja como cabecalho da mensagem. E o que faz o rastreio distribuido atravessar a fila.';
COMMENT ON COLUMN OUTBOX.DATA_PUBLICACAO IS 'Nulo enquanto pendente. Preenchido pelo publicador depois do envio confirmado.';
COMMENT ON TABLE INBOX IS 'Chave de cada mensagem ja processada. Toda entrega e ao menos uma vez; a segunda colide na chave primaria e vira operacao nula.';
COMMENT ON COLUMN INBOX.ID IS 'Chave de idempotencia da mensagem recebida, derivada do evento de origem.';

CREATE INDEX IF NOT EXISTS IX_OUTBOX_PENDENTES ON OUTBOX (DATA_CRIACAO) WHERE DATA_PUBLICACAO IS NULL;

COMMENT ON INDEX IX_OUTBOX_PENDENTES IS 'O publicador le so o que falta enviar. Indice sobre a tabela inteira cresceria para sempre.';
