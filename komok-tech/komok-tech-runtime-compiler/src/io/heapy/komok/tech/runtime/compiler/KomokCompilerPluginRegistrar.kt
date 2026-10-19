package io.heapy.komok.tech.runtime.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

class KomokCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = "io.heapy.komok.runtime"
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(KomokFirExtensionRegistrar())
        IrGenerationExtension.registerExtension(KomokIrGenerationExtension(configuration))
    }
}
