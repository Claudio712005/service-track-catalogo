package com.clau.service_track.catalogo.domain.vo

enum class UnidadeDeMedida(val simbolo: String, val fracionavel: Boolean) {
    UNIDADE("un", false),
    PECA("pc", false),
    LITRO("L", true),
    MILILITRO("mL", true),
    GALAO("gal", true),
    QUILOGRAMA("kg", true),
    GRAMA("g", true),
    METRO("m", true),
    CONJUNTO("cj", false),
}
