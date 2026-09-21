package com.clau.service_track.catalogo.infrastructure.adapter.config

import com.clau.service_track.catalogo.shared.annotation.UseCase
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType

@Configuration
@ComponentScan(
    basePackages = ["com.clau.service_track.catalogo.application"],
    useDefaultFilters = false,
    includeFilters = [ComponentScan.Filter(type = FilterType.ANNOTATION, classes = [UseCase::class])],
)
class UseCaseConfig