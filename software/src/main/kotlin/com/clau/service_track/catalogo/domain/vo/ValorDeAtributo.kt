package com.clau.service_track.catalogo.domain.vo

import java.math.BigDecimal

sealed interface ValorDeAtributo {

    fun comoTexto(): String

    data class Texto(val valor: String) : ValorDeAtributo {
        override fun comoTexto() = valor
    }

    data class Inteiro(val valor: Int) : ValorDeAtributo {
        override fun comoTexto() = valor.toString()
    }

    data class Decimal(val valor: BigDecimal) : ValorDeAtributo {
        override fun comoTexto() = valor.toPlainString()
    }

    data class Booleano(val valor: Boolean) : ValorDeAtributo {
        override fun comoTexto() = valor.toString()
    }

    data class Opcao(val valor: String) : ValorDeAtributo {
        override fun comoTexto() = valor
    }
}
