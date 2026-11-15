SET SEARCH_PATH TO CATALOGO;

INSERT INTO ESTOQUE_SALDOS (INSUMO_ID, QUANTIDADE_DISPONIVEL, QUANTIDADE_RESERVADA, ESTOQUE_MINIMO, UNIDADE_MEDIDA, VERSAO, DATA_ATUALIZACAO) VALUES
    ('018f30bb-77a1-7c22-9b10-2a44de81f0aa', 48.0000, 0.0000, 12.0000, 'LITRO', 0, '2026-03-12T09:00:00Z'),
    ('018f30c4-1d55-7a98-8f03-7bb1c2e4d5f6', 16.0000, 0.0000, 4.0000, 'UNIDADE', 0, '2026-03-12T09:00:00Z'),
    ('018f30d0-9e11-7b44-9c55-3ad2f1b0e7c8', 0.0000, 0.0000, 0.0000, 'LITRO', 0, '2026-03-12T09:00:00Z'),
    ('018f30e2-4c18-7a91-9f36-6ea0b4d2c753', 36.0000, 0.0000, 10.0000, 'LITRO', 0, '2026-03-12T09:00:00Z'),
    ('018f30ea-8b55-7c03-8a47-7fb1c5e3d864', 8.0000, 0.0000, 4.0000, 'UNIDADE', 0, '2026-03-12T09:00:00Z'),
    ('018f30f3-2e67-7d15-9b58-80c2d6f4e975', 24.0000, 0.0000, 6.0000, 'UNIDADE', 0, '2026-03-12T09:00:00Z'),
    ('018f30fb-6f79-7e26-8c69-91d3e7a5fa86', 5.0000, 0.0000, 6.0000, 'UNIDADE', 0, '2026-03-12T09:00:00Z'),
    ('018f3104-1a8b-7f37-9d7a-a2e4f8b60b97', 14.0000, 0.0000, 4.0000, 'UNIDADE', 0, '2026-03-12T09:00:00Z')
ON CONFLICT (INSUMO_ID) DO NOTHING;

COMMENT ON TABLE ESTOQUE_SALDOS IS 'Saldo por insumo. O seed abre com saldo para os insumos ativos e mantem OL-20W50-MIN-1L em zero de proposito: ele e o gatilho de falha da saga, usado para demonstrar compensacao sem mexer em ambiente ao vivo. FL-AR-VW-POLO abre com 5 unidades contra um minimo de 6, abaixo do estoque minimo de proposito, para que o alerta de reposicao tenha caso.';
