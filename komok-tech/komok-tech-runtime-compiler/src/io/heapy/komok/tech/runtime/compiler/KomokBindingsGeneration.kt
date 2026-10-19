package io.heapy.komok.tech.runtime.compiler

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal object BindingDeclarationsKey : GeneratedDeclarationKey()
internal data class BindingPropertyKey(val owner: String, val provider: String) : GeneratedDeclarationKey()

internal class KomokBindingsGeneration(session: FirSession) : FirDeclarationGenerationExtension(session) {
    private val modulePredicate = LookupPredicate.create { annotated(KomokNames.module.asSingleFqName()) }
    private val bindingsName = Name.identifier("Bindings")
    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(modulePredicate)
    }

    private fun moduleOf(bindings: FirClassSymbol<*>): FirRegularClassSymbol? {
        if (bindings.classId.shortClassName != bindingsName) return null
        val outer = bindings.classId.outerClassId ?: return null
        val module = session.symbolProvider.getClassLikeSymbolByClassId(outer) as? FirRegularClassSymbol ?: return null
        return module.takeIf { session.predicateBasedProvider.matches(modulePredicate, it) }
    }

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> =
        if (session.predicateBasedProvider.matches(modulePredicate, classSymbol)) setOf(bindingsName) else emptySet()

    override fun generateNestedClassLikeDeclaration(owner: FirClassSymbol<*>, name: Name, context: NestedClassGenerationContext): FirClassLikeSymbol<*>? =
        if (name == bindingsName && session.predicateBasedProvider.matches(modulePredicate, owner))
            createNestedClass(owner, bindingsName, BindingDeclarationsKey, ClassKind.OBJECT).symbol
        else null

    private fun providers(module: FirRegularClassSymbol): List<FirNamedFunctionSymbol> =
        buildList {
            module.processAllDeclarations(session) { if (it is FirNamedFunctionSymbol) add(it) }
        }.filter { function ->
            (module.classKind == ClassKind.INTERFACE || function.valueParameterSymbols.isEmpty()) &&
            function.resolvedStatus.isSuspend && function.resolvedStatus.visibility == Visibilities.Public &&
                function.resolvedReturnType.classId?.asFqNameString() != "kotlin.Unit" &&
                !function.hasAnnotation(KomokNames.startup, session) &&
                !function.hasAnnotation(KomokNames.shutdown, session)
        }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        val module = moduleOf(classSymbol) ?: return emptySet()
        return providers(module).mapTo(linkedSetOf(SpecialNames.INIT)) { it.name }
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> =
        if (moduleOf(context.owner) != null)
            listOf(createDefaultPrivateConstructor(context.owner, BindingDeclarationsKey).symbol)
        else emptyList()

    override fun generateProperties(callableId: CallableId, context: MemberGenerationContext?): List<FirPropertySymbol> {
        val owner = context?.owner ?: return emptyList()
        val module = moduleOf(owner) ?: return emptyList()
        val provider = providers(module).singleOrNull { it.name == callableId.callableName } ?: return emptyList()
        val bindingType = ClassId(KomokNames.runtimePackage, Name.identifier("Binding"))
            .constructClassLikeType(arrayOf(provider.resolvedReturnType), isMarkedNullable = false)
        return listOf(createMemberProperty(
            owner, BindingPropertyKey(module.classId.asFqNameString(), provider.name.asString()),
            provider.name, bindingType, hasBackingField = true,
        ).symbol)
    }
}
