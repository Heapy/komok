package io.heapy.komok.tech.di.ksp

import com.squareup.kotlinpoet.ClassName

val slf4jLogger = ClassName("org.slf4j", "Logger")
val slf4jLoggerFactory = ClassName("org.slf4j", "LoggerFactory")
val kotlinLazy = ClassName("kotlin", "Lazy")
val moduleDslMarker = ClassName("io.heapy.komok.tech.di.lib", "KomokModuleDsl")
