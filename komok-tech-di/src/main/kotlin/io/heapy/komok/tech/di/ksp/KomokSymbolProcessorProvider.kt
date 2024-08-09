package io.heapy.komok.tech.di.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class KomokSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment,
    ): SymbolProcessor {
        return KomokSymbolProcessor(
            logger = environment.logger,
            codeGenerator = environment.codeGenerator,
        )
    }
}
