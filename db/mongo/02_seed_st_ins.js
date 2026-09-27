const categorias = [
    {
        _id: "018f2c9a-5f2e-7c31-9a41-6f3b2d0e9c11",
        CODIGO: "OLEO_MOTOR",
        NOME: "Óleo de motor",
        UNIDADE_PADRAO: "LITRO",
        ATRIBUTOS: [
            { CHAVE: "viscosidade", ROTULO: "Viscosidade", TIPO: "OPCAO", OBRIGATORIO: true, OPCOES: ["0W20", "5W30", "10W40", "15W40"], ORDEM: 0 },
            { CHAVE: "especificacao-api", ROTULO: "Especificação API", TIPO: "TEXTO", OBRIGATORIO: true, OPCOES: [], ORDEM: 0 },
            { CHAVE: "sintetico", ROTULO: "Sintético", TIPO: "BOOLEANO", OBRIGATORIO: false, OPCOES: [], ORDEM: 0 }
        ],
        DATA_CRIACAO: ISODate("2026-09-01T11:00:00Z"),
        DATA_ATUALIZACAO: ISODate("2026-09-01T11:00:00Z")
    },
    {
        _id: "018f2ca1-2b77-7f10-8c02-91ab7d4e5f20",
        CODIGO: "PNEU",
        NOME: "Pneu",
        UNIDADE_PADRAO: "UNIDADE",
        ATRIBUTOS: [
            { CHAVE: "aro", ROTULO: "Aro", TIPO: "INTEIRO", UNIDADE: "pol", OBRIGATORIO: true, OPCOES: [], ORDEM: 0 },
            { CHAVE: "indice-carga", ROTULO: "Índice de carga", TIPO: "INTEIRO", OBRIGATORIO: false, OPCOES: [], ORDEM: 1 }
        ],
        DATA_CRIACAO: ISODate("2026-09-01T11:05:00Z"),
        DATA_ATUALIZACAO: ISODate("2026-09-01T11:05:00Z")
    }
];

const insumos = [
    {
        _id: "018f30bb-77a1-7c22-9b10-2a44de81f0aa",
        CATEGORIA_ID: "018f2c9a-5f2e-7c31-9a41-6f3b2d0e9c11",
        SKU: "OL-5W30-SYN-1L",
        NOME: "Óleo 5W30 sintético 1L",
        DESCRICAO: "Óleo lubrificante sintético para motores a gasolina e flex, embalagem de 1 litro.",
        MARCA: "Lubrax",
        FABRICANTE: "Petrobras",
        CODIGO_FABRICANTE: "LB-5W30-1L",
        CODIGO_BARRAS: "7891234567890",
        UNIDADE_MEDIDA: "LITRO",
        CUSTO_PADRAO: NumberDecimal("38.90"),
        CONTROLA_LOTE: true,
        VALIDADE_EM_DIAS: 730,
        ESPECIFICACAO: { viscosidade: "5W30", "especificacao-api": "SN", sintetico: "true" },
        ATIVO: true,
        DATA_CRIACAO: ISODate("2026-09-01T12:00:00Z"),
        DATA_ATUALIZACAO: ISODate("2026-09-01T12:00:00Z")
    },
    {
        _id: "018f30c4-1d55-7a98-8f03-7bb1c2e4d5f6",
        CATEGORIA_ID: "018f2ca1-2b77-7f10-8c02-91ab7d4e5f20",
        SKU: "PN-205-55-R16",
        NOME: "Pneu 205/55 R16",
        DESCRICAO: "Pneu radial para automóvel de passeio.",
        MARCA: "Pirelli",
        UNIDADE_MEDIDA: "UNIDADE",
        CUSTO_PADRAO: NumberDecimal("459.90"),
        CONTROLA_LOTE: false,
        ESPECIFICACAO: { aro: "16", "indice-carga": "91" },
        ATIVO: true,
        DATA_CRIACAO: ISODate("2026-09-02T09:00:00Z"),
        DATA_ATUALIZACAO: ISODate("2026-09-02T09:00:00Z")
    },
    {
        _id: "018f30d0-9e11-7b44-9c55-3ad2f1b0e7c8",
        CATEGORIA_ID: "018f2c9a-5f2e-7c31-9a41-6f3b2d0e9c11",
        SKU: "OL-20W50-MIN-1L",
        NOME: "Óleo 20W50 mineral 1L",
        DESCRICAO: "Linha descontinuada pelo fabricante.",
        UNIDADE_MEDIDA: "LITRO",
        CUSTO_PADRAO: NumberDecimal("24.50"),
        CONTROLA_LOTE: false,
        ESPECIFICACAO: { viscosidade: "15W40", "especificacao-api": "SL" },
        ATIVO: false,
        DATA_CRIACAO: ISODate("2026-02-10T17:00:00Z"),
        DATA_ATUALIZACAO: ISODate("2026-08-20T14:00:00Z")
    }
];

categorias.forEach(c => db.CATEGORIAS.replaceOne({ _id: c._id }, c, { upsert: true }));
insumos.forEach(i => db.INSUMOS.replaceOne({ _id: i._id }, i, { upsert: true }));

print("ST_INS seed: " + db.CATEGORIAS.countDocuments() + " categorias, " + db.INSUMOS.countDocuments() + " insumos.");
