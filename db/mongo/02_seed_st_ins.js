const categorias = [
    {
        _id: "018f2c9a-5f2e-7c31-9a41-6f3b2d0e9c11",
        CODIGO: "OLEO_MOTOR",
        NOME: "Óleo de motor",
        UNIDADE_PADRAO: "LITRO",
        ATIVA: true,
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
        ATIVA: true,
        ATRIBUTOS: [
            { CHAVE: "aro", ROTULO: "Aro", TIPO: "INTEIRO", UNIDADE: "pol", OBRIGATORIO: true, OPCOES: [], ORDEM: 0 },
            { CHAVE: "indice-carga", ROTULO: "Índice de carga", TIPO: "INTEIRO", OBRIGATORIO: false, OPCOES: [], ORDEM: 1 }
        ],
        DATA_CRIACAO: ISODate("2026-09-01T11:05:00Z"),
        DATA_ATUALIZACAO: ISODate("2026-09-01T11:05:00Z")
    },
    {
        _id: "018f2cb8-7d41-7e55-9d13-4ca8e2f7b931",
        CODIGO: "FILTRO",
        NOME: "Filtro",
        UNIDADE_PADRAO: "UNIDADE",
        ATIVA: true,
        ATRIBUTOS: [
            { CHAVE: "aplicacao", ROTULO: "Aplicação", TIPO: "TEXTO", OBRIGATORIO: true, OPCOES: [], ORDEM: 0 },
            { CHAVE: "tipo", ROTULO: "Tipo", TIPO: "OPCAO", OBRIGATORIO: true, OPCOES: ["OLEO", "AR", "COMBUSTIVEL", "CABINE"], ORDEM: 1 }
        ],
        DATA_CRIACAO: ISODate("2026-09-01T11:10:00Z"),
        DATA_ATUALIZACAO: ISODate("2026-09-01T11:10:00Z")
    },
    {
        _id: "018f2cc0-9a2e-7b07-8e24-5db9f3a1c842",
        CODIGO: "FREIO",
        NOME: "Freio",
        UNIDADE_PADRAO: "UNIDADE",
        ATIVA: true,
        ATRIBUTOS: [
            { CHAVE: "posicao", ROTULO: "Posição", TIPO: "OPCAO", OBRIGATORIO: true, OPCOES: ["DIANTEIRA", "TRASEIRA"], ORDEM: 0 },
            { CHAVE: "material", ROTULO: "Material", TIPO: "TEXTO", OBRIGATORIO: false, OPCOES: [], ORDEM: 1 }
        ],
        DATA_CRIACAO: ISODate("2026-09-01T11:15:00Z"),
        DATA_ATUALIZACAO: ISODate("2026-09-01T11:15:00Z")
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
    },
    {
        _id: "018f30e2-4c18-7a91-9f36-6ea0b4d2c753",
        CATEGORIA_ID: "018f2c9a-5f2e-7c31-9a41-6f3b2d0e9c11",
        SKU: "OL-10W40-SEMI-1L",
        NOME: "Óleo 10W40 semissintético 1L",
        DESCRICAO: "Óleo lubrificante semissintético, embalagem de 1 litro.",
        MARCA: "Mobil",
        FABRICANTE: "ExxonMobil",
        CODIGO_FABRICANTE: "MB-10W40-1L",
        UNIDADE_MEDIDA: "LITRO",
        CUSTO_PADRAO: NumberDecimal("31.50"),
        CONTROLA_LOTE: true,
        VALIDADE_EM_DIAS: 730,
        ESPECIFICACAO: { viscosidade: "10W40", "especificacao-api": "SL", sintetico: "false" },
        ATIVO: true,
        DATA_CRIACAO: ISODate("2026-09-03T10:00:00Z"),
        DATA_ATUALIZACAO: ISODate("2026-09-03T10:00:00Z")
    },
    {
        _id: "018f30ea-8b55-7c03-8a47-7fb1c5e3d864",
        CATEGORIA_ID: "018f2ca1-2b77-7f10-8c02-91ab7d4e5f20",
        SKU: "PN-195-65-R15",
        NOME: "Pneu 195/65 R15",
        DESCRICAO: "Pneu radial para automóvel compacto.",
        MARCA: "Goodyear",
        UNIDADE_MEDIDA: "UNIDADE",
        CUSTO_PADRAO: NumberDecimal("389.90"),
        CONTROLA_LOTE: false,
        ESPECIFICACAO: { aro: "15", "indice-carga": "88" },
        ATIVO: true,
        DATA_CRIACAO: ISODate("2026-09-03T10:30:00Z"),
        DATA_ATUALIZACAO: ISODate("2026-09-03T10:30:00Z")
    },
    {
        _id: "018f30f3-2e67-7d15-9b58-80c2d6f4e975",
        CATEGORIA_ID: "018f2cb8-7d41-7e55-9d13-4ca8e2f7b931",
        SKU: "FL-OLEO-GM-ONIX",
        NOME: "Filtro de óleo Onix 1.0",
        DESCRICAO: "Filtro de óleo para motor 1.0 de três cilindros.",
        MARCA: "Tecfil",
        UNIDADE_MEDIDA: "UNIDADE",
        CUSTO_PADRAO: NumberDecimal("28.70"),
        CONTROLA_LOTE: false,
        ESPECIFICACAO: { aplicacao: "Chevrolet Onix 1.0 2017-2024", tipo: "OLEO" },
        ATIVO: true,
        DATA_CRIACAO: ISODate("2026-09-04T09:00:00Z"),
        DATA_ATUALIZACAO: ISODate("2026-09-04T09:00:00Z")
    },
    {
        _id: "018f30fb-6f79-7e26-8c69-91d3e7a5fa86",
        CATEGORIA_ID: "018f2cb8-7d41-7e55-9d13-4ca8e2f7b931",
        SKU: "FL-AR-VW-POLO",
        NOME: "Filtro de ar Polo TSI",
        DESCRICAO: "Filtro de ar do motor para a linha Polo com motor TSI.",
        MARCA: "Mann",
        UNIDADE_MEDIDA: "UNIDADE",
        CUSTO_PADRAO: NumberDecimal("64.00"),
        CONTROLA_LOTE: false,
        ESPECIFICACAO: { aplicacao: "Volkswagen Polo 1.0 TSI 2018-2024", tipo: "AR" },
        ATIVO: true,
        DATA_CRIACAO: ISODate("2026-09-04T09:20:00Z"),
        DATA_ATUALIZACAO: ISODate("2026-09-04T09:20:00Z")
    },
    {
        _id: "018f3104-1a8b-7f37-9d7a-a2e4f8b60b97",
        CATEGORIA_ID: "018f2cc0-9a2e-7b07-8e24-5db9f3a1c842",
        SKU: "PA-FREIO-DIANT-CER",
        NOME: "Pastilha de freio dianteira cerâmica",
        DESCRICAO: "Jogo de pastilhas dianteiras de composto cerâmico.",
        MARCA: "Bosch",
        UNIDADE_MEDIDA: "UNIDADE",
        CUSTO_PADRAO: NumberDecimal("187.40"),
        CONTROLA_LOTE: false,
        ESPECIFICACAO: { posicao: "DIANTEIRA", material: "Cerâmico" },
        ATIVO: true,
        DATA_CRIACAO: ISODate("2026-09-05T14:00:00Z"),
        DATA_ATUALIZACAO: ISODate("2026-09-05T14:00:00Z")
    }
];

categorias.forEach(c => db.CATEGORIAS.replaceOne({ _id: c._id }, c, { upsert: true }));
insumos.forEach(i => db.INSUMOS.replaceOne({ _id: i._id }, Object.assign({ VERSAO: 0 }, i), { upsert: true }));

print("ST_INS seed: " + db.CATEGORIAS.countDocuments() + " categorias, " + db.INSUMOS.countDocuments() + " insumos.");
