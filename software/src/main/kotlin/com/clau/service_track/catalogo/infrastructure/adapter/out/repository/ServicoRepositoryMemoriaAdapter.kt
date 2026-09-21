package com.clau.service_track.catalogo.infrastructure.adapter.out.repository

import com.clau.service_track.catalogo.application.port.out.repository.ServicoRepositoryPort
import com.clau.service_track.catalogo.domain.model.Servico
import com.clau.service_track.catalogo.domain.vo.DomainId
import com.clau.service_track.catalogo.domain.vo.ValorMonetario
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Repository
class ServicoRepositoryMemoriaAdapter : ServicoRepositoryPort {

    private val acervo = ConcurrentHashMap<String, Servico>()

    init {
        catalogoInicial().forEach { acervo[it.id.value] = it }
    }

    override fun salvar(servico: Servico): Servico {
        acervo[servico.id.value] = servico
        return servico
    }

    override fun buscarPorId(id: DomainId): Servico? = acervo[id.value]

    override fun listar(incluirInativos: Boolean): List<Servico> = acervo.values
        .filter { incluirInativos || it.ativo }
        .toList()

    override fun existeComNome(nome: String): Boolean = acervo.values
        .any { it.ativo && it.nome.equals(nome.trim(), ignoreCase = true) }

    private fun catalogoInicial(): List<Servico> = listOf(
        montar(
            id = "bbfdb1a8-66e2-4292-a6cb-6a6d3fb080fd",
            nome = "Troca de óleo e filtro",
            descricao = "Substituição do óleo lubrificante do motor e do filtro de óleo, " +
                "com verificação de nível dos demais fluidos e descarte do material usado.",
            valor = "189.90",
            criadoEm = LocalDateTime.of(2026, 3, 12, 9, 14, 0),
        ),
        montar(
            id = "4b0a0a22-4dd7-4367-95d7-6a3583b4c0ea",
            nome = "Alinhamento e balanceamento",
            descricao = "Alinhamento da geometria de direção e balanceamento das quatro rodas " +
                "em equipamento computadorizado.",
            valor = "149.90",
            criadoEm = LocalDateTime.of(2026, 3, 12, 9, 22, 0),
        ),
        montar(
            id = "2212f1a2-921d-4634-ba5f-85bb0b462063",
            nome = "Revisão do sistema de freios",
            descricao = "Inspeção de pastilhas, discos, tambores e fluido de freio, " +
                "com sangria do sistema quando necessário. Não inclui peças.",
            valor = "320.00",
            criadoEm = LocalDateTime.of(2026, 4, 2, 14, 5, 0),
        ),
        montar(
            id = "d38c0eec-0433-4d8c-9905-383df6f5358a",
            nome = "Troca de correia dentada",
            descricao = "Substituição da correia dentada, tensor e rolamentos, " +
                "com sincronização do comando de válvulas.",
            valor = "780.00",
            criadoEm = LocalDateTime.of(2026, 4, 18, 8, 40, 0),
        ),
        montar(
            id = "5d00cdcd-2e96-45be-98b2-c3ba803cb15a",
            nome = "Diagnóstico eletrônico",
            descricao = "Leitura da central eletrônica com scanner automotivo, " +
                "interpretação dos códigos de falha e relatório ao cliente.",
            valor = "180.00",
            criadoEm = LocalDateTime.of(2026, 5, 7, 11, 30, 0),
        ),
        montar(
            id = "d1fd0165-faa4-4b18-a109-29eb4140fd9a",
            nome = "Revisão de suspensão",
            descricao = "Inspeção de amortecedores, molas, batentes, bieletas e buchas, " +
                "com teste de rodagem.",
            valor = "450.00",
            criadoEm = LocalDateTime.of(2026, 5, 21, 15, 12, 0),
        ),
        montar(
            id = "f8bdd6bb-3552-4abd-bef3-eb6e64cf5612",
            nome = "Higienização do ar-condicionado",
            descricao = "Limpeza do sistema de climatização, troca do filtro de cabine " +
                "e aplicação de bactericida no evaporador.",
            valor = "220.00",
            criadoEm = LocalDateTime.of(2026, 6, 3, 10, 0, 0),
        ),
        montar(
            id = "6dc34799-eaa5-4925-b89a-4d7c6fa6376f",
            nome = "Troca de embreagem",
            descricao = "Substituição do kit de embreagem com remoção da caixa de câmbio. " +
                "Serviço descontinuado: passou a ser encaminhado à unidade de mecânica pesada.",
            valor = "1250.00",
            criadoEm = LocalDateTime.of(2026, 1, 9, 13, 45, 0),
            ativo = false,
        ),
    )

    private fun montar(
        id: String,
        nome: String,
        descricao: String,
        valor: String,
        criadoEm: LocalDateTime,
        ativo: Boolean = true,
    ): Servico = Servico.reconstituir(
        id = DomainId.de(id),
        nome = nome,
        descricao = descricao,
        valorReferencia = ValorMonetario.de(valor),
        dataCriacao = criadoEm,
        dataAtualizacao = criadoEm,
        ativo = ativo,
    )
}
