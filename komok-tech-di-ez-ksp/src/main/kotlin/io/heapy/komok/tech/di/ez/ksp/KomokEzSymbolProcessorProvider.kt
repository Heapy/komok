package io.heapy.komok.tech.di.ez.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class KomokEzSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return KomokEzSymbolProcessor(
            logger = environment.logger,
            codeGenerator = environment.codeGenerator,
        )
    }
}
