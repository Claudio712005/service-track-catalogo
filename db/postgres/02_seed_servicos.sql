SET SEARCH_PATH TO CATALOGO;

INSERT INTO SERVICOS (ID, NOME_SERVICO, DESCRICAO_SERVICO, VALOR_REFERENCIA, ATIVO, DATA_CRIACAO, DATA_ATUALIZACAO) VALUES
    ('bbfdb1a8-66e2-4292-a6cb-6a6d3fb080fd', 'Troca de óleo e filtro', 'Substituição do óleo lubrificante do motor e do filtro de óleo, com verificação de nível dos demais fluidos e descarte do material usado.', 189.90, TRUE, '2026-03-12T09:14:00Z', '2026-03-12T09:14:00Z'),
    ('4b0a0a22-4dd7-4367-95d7-6a3583b4c0ea', 'Alinhamento e balanceamento', 'Alinhamento da geometria de direção e balanceamento das quatro rodas em equipamento computadorizado.', 149.90, TRUE, '2026-03-12T09:22:00Z', '2026-03-12T09:22:00Z'),
    ('2212f1a2-921d-4634-ba5f-85bb0b462063', 'Revisão do sistema de freios', 'Inspeção de pastilhas, discos, tambores e fluido de freio, com sangria do sistema quando necessário. Não inclui peças.', 320.00, TRUE, '2026-04-02T14:05:00Z', '2026-04-02T14:05:00Z'),
    ('d38c0eec-0433-4d8c-9905-383df6f5358a', 'Troca de correia dentada', 'Substituição da correia dentada, tensor e rolamentos, com sincronização do comando de válvulas.', 780.00, TRUE, '2026-04-18T08:40:00Z', '2026-04-18T08:40:00Z'),
    ('5d00cdcd-2e96-45be-98b2-c3ba803cb15a', 'Diagnóstico eletrônico', 'Leitura da central eletrônica com scanner automotivo, interpretação dos códigos de falha e relatório ao cliente.', 180.00, TRUE, '2026-05-07T11:30:00Z', '2026-05-07T11:30:00Z'),
    ('d1fd0165-faa4-4b18-a109-29eb4140fd9a', 'Revisão de suspensão', 'Inspeção de amortecedores, molas, batentes, bieletas e buchas, com teste de rodagem.', 450.00, TRUE, '2026-05-21T15:12:00Z', '2026-05-21T15:12:00Z'),
    ('f8bdd6bb-3552-4abd-bef3-eb6e64cf5612', 'Higienização do ar-condicionado', 'Limpeza do sistema de climatização, troca do filtro de cabine e aplicação de bactericida no evaporador.', 220.00, TRUE, '2026-06-03T10:00:00Z', '2026-06-03T10:00:00Z'),
    ('6dc34799-eaa5-4925-b89a-4d7c6fa6376f', 'Troca de embreagem', 'Substituição do kit de embreagem com remoção da caixa de câmbio. Serviço descontinuado: passou a ser encaminhado à unidade de mecânica pesada.', 1250.00, FALSE, '2026-01-09T13:45:00Z', '2026-01-09T13:45:00Z')
ON CONFLICT (ID) DO NOTHING;
