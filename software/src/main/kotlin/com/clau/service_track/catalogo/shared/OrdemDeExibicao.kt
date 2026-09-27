package com.clau.service_track.catalogo.shared

import java.text.Collator
import java.util.Locale

object OrdemDeExibicao {

    private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

    private val colacao: Collator = Collator.getInstance(PT_BR).apply {
        strength = Collator.SECONDARY
    }

    fun porNome(): Comparator<String> = Comparator { esquerda, direita -> colacao.compare(esquerda, direita) }
}
