const colecoes = db.getCollectionNames();

const definicaoCategorias = {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            title: "Categoria de insumo e os atributos que ela exige",
            required: ["_id", "CODIGO", "NOME", "UNIDADE_PADRAO", "ATIVA", "ATRIBUTOS", "DATA_CRIACAO"],
            properties: {
                _id: { bsonType: "string", description: "UUID v7 em texto, gerado pela aplicacao." },
                CODIGO: { bsonType: "string", minLength: 2, maxLength: 40, description: "Codigo estavel da categoria, em maiuscula. Ex: OLEO_MOTOR." },
                NOME: { bsonType: "string", minLength: 2, maxLength: 120, description: "Nome exibido na tela." },
                UNIDADE_PADRAO: {
                    enum: ["UNIDADE", "PECA", "LITRO", "MILILITRO", "GALAO", "QUILOGRAMA", "GRAMA", "METRO", "CONJUNTO"],
                    description: "Unidade sugerida ao cadastrar insumo desta categoria."
                },
                ATIVA: { bsonType: "bool", description: "Falso retira a categoria e os insumos dela da listagem padrao, e barra cadastro de insumo novo. Nada e apagado." },
                ATRIBUTOS: {
                    bsonType: "array",
                    description: "Definicoes que o usuario da oficina cria em runtime. E dado, nao schema: atributo novo nao exige deploy.",
                    items: {
                        bsonType: "object",
                        required: ["CHAVE", "ROTULO", "TIPO", "OBRIGATORIO"],
                        properties: {
                            CHAVE: { bsonType: "string", pattern: "^[a-z][a-z0-9-]*$", description: "Identificador do atributo na categoria, em minusculas. Unico nela, e e a chave usada no mapa ESPECIFICACAO do insumo." },
                            ROTULO: { bsonType: "string", description: "Nome do atributo na tela." },
                            TIPO: { enum: ["TEXTO", "INTEIRO", "DECIMAL", "BOOLEANO", "OPCAO"], description: "Tipo do valor, validado pelo dominio ao especificar o insumo." },
                            OBRIGATORIO: { bsonType: "bool", description: "Atributo novo nunca nasce obrigatorio: o acervo ja cadastrado nao o possui." },
                            UNIDADE: { bsonType: "string", description: "Unidade exibida junto do valor. Nao participa de calculo." },
                            OPCOES: { bsonType: "array", items: { bsonType: "string" }, description: "Valores admitidos quando TIPO = OPCAO." },
                            ORDEM: { bsonType: "int", description: "Posicao na tela." }
                        }
                    }
                },
                DATA_CRIACAO: { bsonType: "date", description: "Instante do cadastro, em UTC." },
                DATA_ATUALIZACAO: { bsonType: "date", description: "Instante da ultima alteracao, em UTC." }
            }
        }
    },
    validationLevel: "strict",
    validationAction: "error"
};

const definicaoInsumos = {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            title: "Insumo: o que e o material e quais sao suas caracteristicas. Quantidade NAO mora aqui, e sim no Postgres ST_CAT.",
            required: ["_id", "CATEGORIA_ID", "SKU", "NOME", "UNIDADE_MEDIDA", "CUSTO_PADRAO", "ATIVO", "DATA_CRIACAO"],
            properties: {
                _id: { bsonType: "string", description: "UUID v7 em texto. E o mesmo valor que ESTOQUE_SALDOS.INSUMO_ID guarda no Postgres." },
                CATEGORIA_ID: { bsonType: "string", description: "Referencia a categoria. Sem integridade referencial: o dominio valida antes de gravar." },
                SKU: { bsonType: "string", minLength: 3, maxLength: 40, description: "Codigo interno unico. E a busca do balcao." },
                NOME: { bsonType: "string", minLength: 2, maxLength: 160, description: "Nome comercial do material." },
                DESCRICAO: { bsonType: "string", description: "Texto livre com detalhe tecnico." },
                MARCA: { bsonType: "string", description: "Marca do produto." },
                FABRICANTE: { bsonType: "string", description: "Fabricante, quando difere da marca." },
                CODIGO_FABRICANTE: { bsonType: "string", description: "Codigo do item no catalogo do fabricante." },
                CODIGO_BARRAS: { bsonType: "string", pattern: "^[0-9]{8,14}$", description: "EAN ou GTIN, para leitura por scanner." },
                UNIDADE_MEDIDA: {
                    enum: ["UNIDADE", "PECA", "LITRO", "MILILITRO", "GALAO", "QUILOGRAMA", "GRAMA", "METRO", "CONJUNTO"],
                    description: "Unidade de movimentacao. Imutavel depois do primeiro movimento de estoque, porque mudaria o significado das quantidades gravadas."
                },
                CUSTO_PADRAO: { bsonType: "decimal", minimum: 0, description: "Custo de tabela em reais, como Decimal128. Nunca double." },
                CONTROLA_LOTE: { bsonType: "bool", description: "Verdadeiro exige lote e validade na entrada, e consumo pelo que vence primeiro." },
                VALIDADE_EM_DIAS: { bsonType: "int", minimum: 0, description: "Prazo de validade tipico, usado para sugerir a data na entrada." },
                ESPECIFICACAO: { bsonType: "object", description: "Valores dos atributos definidos pela categoria. Chaves e tipos validados pelo dominio contra a definicao, nao pelo banco." },
                ATIVO: { bsonType: "bool", description: "Falso bloqueia nova reserva e novo orcamento; saldo existente permanece." },
                VERSAO: { bsonType: ["int", "long"], description: "Contador de alteracao mantido pelo Spring Data, para deteccao de escrita concorrente." },
                DATA_CRIACAO: { bsonType: "date", description: "Instante do cadastro, em UTC." },
                DATA_ATUALIZACAO: { bsonType: "date", description: "Instante da ultima alteracao, em UTC." }
            }
        }
    },
    validationLevel: "strict",
    validationAction: "error"
};

if (colecoes.includes("CATEGORIAS")) {
    db.runCommand(Object.assign({ collMod: "CATEGORIAS" }, definicaoCategorias));
} else {
    db.createCollection("CATEGORIAS", definicaoCategorias);
}

if (colecoes.includes("INSUMOS")) {
    db.runCommand(Object.assign({ collMod: "INSUMOS" }, definicaoInsumos));
} else {
    db.createCollection("INSUMOS", definicaoInsumos);
}

db.CATEGORIAS.createIndex({ CODIGO: 1 }, { name: "UQ_CATEGORIAS_CODIGO", unique: true });
db.CATEGORIAS.createIndex({ NOME: 1 }, { name: "IX_CATEGORIAS_NOME" });

db.INSUMOS.createIndex({ SKU: 1 }, { name: "UQ_INSUMOS_SKU", unique: true });
db.INSUMOS.createIndex({ CODIGO_BARRAS: 1 }, { name: "UQ_INSUMOS_CODIGO_BARRAS", unique: true, partialFilterExpression: { CODIGO_BARRAS: { $exists: true } } });
db.INSUMOS.createIndex({ CATEGORIA_ID: 1, NOME: 1 }, { name: "IX_INSUMOS_CATEGORIA_NOME" });
db.INSUMOS.createIndex({ ATIVO: 1, NOME: 1 }, { name: "IX_INSUMOS_ATIVOS", partialFilterExpression: { ATIVO: true } });

print("ST_INS: " + db.getCollectionNames().length + " colecoes, indices aplicados.");
