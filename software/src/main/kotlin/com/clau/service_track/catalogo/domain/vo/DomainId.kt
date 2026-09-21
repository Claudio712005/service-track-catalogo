package com.clau.service_track.catalogo.domain.vo

import com.clau.service_track.catalogo.domain.exception.DomainException
import java.util.UUID

@JvmInline
value class DomainId private constructor(val value: String) {

    override fun toString(): String = value

    companion object {

        fun gerar(): DomainId = DomainId(UUID.randomUUID().toString())

        fun de(value: String): DomainId {
            if (value.isBlank()) {
                throw DomainException("Identificador não pode ser vazio")
            }
            return DomainId(value)
        }
    }
}
