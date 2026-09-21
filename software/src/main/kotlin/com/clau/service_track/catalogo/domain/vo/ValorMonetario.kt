package com.clau.service_track.catalogo.domain.vo

import com.clau.service_track.catalogo.domain.exception.DomainException
import java.math.BigDecimal
import java.math.RoundingMode

@JvmInline
value class ValorMonetario private constructor(val valor: BigDecimal) {

    operator fun plus(outro: ValorMonetario): ValorMonetario = de(valor + outro.valor)

    operator fun times(fator: Int): ValorMonetario = de(valor * fator.toBigDecimal())

    operator fun times(fator: BigDecimal): ValorMonetario = de(valor * fator)

    operator fun compareTo(outro: ValorMonetario): Int = valor.compareTo(outro.valor)

    override fun toString(): String = valor.toPlainString()

    companion object {

        private const val CASAS_DECIMAIS = 2

        fun de(valor: BigDecimal): ValorMonetario {
            if (valor < BigDecimal.ZERO) {
                throw DomainException("Valor monetário não pode ser negativo")
            }
            return ValorMonetario(valor.setScale(CASAS_DECIMAIS, RoundingMode.HALF_UP))
        }

        fun de(valor: String): ValorMonetario = de(BigDecimal(valor))

        fun zero(): ValorMonetario = de(BigDecimal.ZERO)
    }
}
