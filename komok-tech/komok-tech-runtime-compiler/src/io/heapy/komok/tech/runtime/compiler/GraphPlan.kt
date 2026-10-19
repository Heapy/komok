package io.heapy.komok.tech.runtime.compiler

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*

internal data class GraphBinding(
    val owner: String,
    val name: String,
    val type: IrType,
    val target: IrFunction,
    val module: IrClass? = null,
    val objectOwner: IrClass? = null,
    val intoSet: Boolean = false,
) {
    val address: String = "$owner.$name"
    val descriptor: String = target.providerDescriptor()
    val parameters: List<IrValueParameter> = target.parameters.filter { it.kind == IrParameterKind.Regular }
}

internal fun IrFunction.providerDescriptor(): String {
    val values = parameters.filter { it.kind == IrParameterKind.Regular }
    return if (values.isEmpty()) "" else values.joinToString(", ", "(", ")") { it.type.typeKey() }
}

internal fun validateBindingDependencies(bindings: List<GraphBinding>): List<GraphBinding> {
    val byType = bindings.filterNot { it.intoSet }.groupBy { it.type.typeKey() }
    val contributions = bindings.filter { it.intoSet }.groupBy { it.type.typeKey() }
    val completed = mutableSetOf<GraphBinding>()
    val path = mutableListOf<GraphBinding>()
    fun visit(binding: GraphBinding) {
        if (binding in completed) return
        komokRequire(binding !in path) { "Komok dependency cycle: ${(path + binding).joinToString(" -> ") { it.address }}" }
        path.add(binding)
        binding.parameters.forEach { parameter ->
            val key = parameter.type.typeKey()
            val candidates = byType[key].orEmpty()
            val setElement = (parameter.type as? IrSimpleType)?.takeIf {
                it.classFqName?.asString() == "kotlin.collections.Set"
            }?.arguments?.singleOrNull()?.typeOrNull?.typeKey()
            val dependencies = when {
                candidates.size == 1 -> candidates
                candidates.size > 1 -> komokError("Multiple bindings for ${parameter.name}: $key in ${binding.address}: ${candidates.joinToString { it.address }}")
                setElement != null && contributions.containsKey(setElement) -> contributions.getValue(setElement)
                else -> komokError("No binding for ${parameter.name}: $key. Dependency chain: ${path.joinToString(" -> ") { it.address }} -> $key")
            }
            dependencies.forEach(::visit)
        }
        path.removeAt(path.lastIndex)
        completed.add(binding)
    }
    bindings.forEach(::visit)
    return completed.toList()
}
