package io.heapy.komok.tech.runtime.compiler

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirNamedFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType

internal class KomokFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::KomokCheckers
        +::KomokBindingsGeneration
    }
}

private class KomokCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers = object : DeclarationCheckers() {
        override val namedFunctionCheckers = setOf(KomokProviderChecker)
        override val regularClassCheckers = setOf(KomokModuleChecker)
    }
}

private object KomokProviderChecker : FirNamedFunctionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirNamedFunction) {
        val owner = context.containingDeclarations.lastOrNull { it is FirRegularClassSymbol } as? FirRegularClassSymbol ?: return
        if (!owner.hasAnnotation(KomokNames.module, context.session)) return
        if (declaration.hasAnnotation(KomokNames.startup, context.session) ||
            declaration.hasAnnotation(KomokNames.shutdown, context.session)) {
            if (!declaration.isSuspend || declaration.returnTypeRef.coneType.classId?.asFqNameString() != "kotlin.Unit" ||
                declaration.valueParameters.isNotEmpty() || declaration.status.visibility != Visibilities.Public) {
                reporter.reportOn(declaration.source, FirErrors.UNSUPPORTED,
                    "Komok lifecycle hooks must be public suspend functions returning Unit with no value parameters.")
            }
            return
        }
        if (!declaration.isSuspend || declaration.status.visibility != Visibilities.Public) return
        if (owner.classKind != ClassKind.INTERFACE && declaration.valueParameters.isNotEmpty()) return
        if (declaration.returnTypeRef.coneType.classId?.asFqNameString() == "kotlin.Unit") return
        if (declaration.body == null) {
            reporter.reportOn(declaration.source, FirErrors.UNSUPPORTED,
                "Komok providers must have a body, including default methods in interface modules.")
        }
        val contexts = declaration.contextParameters
        if (contexts.size != 1 || contexts.single().returnTypeRef.coneType.classId != KomokNames.context) {
            reporter.reportOn(declaration.source, FirErrors.UNSUPPORTED,
                "Komok provider requires exactly one context parameter: context(_: KomokContext).")
        }
        if (declaration.typeParameters.isNotEmpty()) {
            reporter.reportOn(declaration.source, FirErrors.UNSUPPORTED,
                "Komok providers must have concrete types; generic provider functions are not supported.")
        }
    }
}

private object KomokModuleChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        if (!declaration.hasAnnotation(KomokNames.module, context.session)) return
        fun problem(message: String) { reporter.reportOn(declaration.source, FirErrors.UNSUPPORTED, message) }
        if (declaration.classKind !in setOf(ClassKind.CLASS, ClassKind.INTERFACE) || declaration.typeParameters.isNotEmpty() || declaration.status.isInner) {
            problem("Komok modules must be non-inner classes or interfaces with concrete types.")
        }
        val path = linkedSetOf<FirRegularClassSymbol>()
        val completed = mutableSetOf<FirRegularClassSymbol>()
        fun visit(module: FirRegularClassSymbol) {
            if (module in completed) return
            if (!path.add(module)) {
                problem("Komok module cycle: ${(path.toList() + module).joinToString(" -> ") { it.classId.asFqNameString() }}")
                return
            }
            val parameters = module.primaryConstructorIfAny(context.session)?.valueParameterSymbols.orEmpty()
            val types = parameters.map { it.resolvedReturnType }
            if (types.distinct().size != types.size) problem("Module ${module.classId} has duplicate module constructor types.")
            parameters.forEach { parameter ->
                val dependency = parameter.resolvedReturnType.toRegularClassSymbol(context.session)
                if (dependency == null || !dependency.hasAnnotation(KomokNames.module, context.session)) {
                    problem("Module constructor parameter ${parameter.name} must reference another @Module.")
                } else visit(dependency)
            }
            path.remove(module)
            completed.add(module)
        }
        visit(declaration.symbol)
        val providers = mutableListOf<FirNamedFunctionSymbol>()
        declaration.symbol.processAllDeclarations(context.session) {
            if (it is FirNamedFunctionSymbol && it.resolvedStatus.isSuspend &&
                it.resolvedStatus.visibility == Visibilities.Public &&
                it.resolvedReturnType.classId?.asFqNameString() != "kotlin.Unit" &&
                (declaration.classKind == ClassKind.INTERFACE || it.valueParameterSymbols.isEmpty())) providers.add(it)
        }
        if (providers.groupBy { it.name }.any { it.value.size > 1 }) {
            problem("Komok provider names must be unique within a module.")
        }
    }
}
