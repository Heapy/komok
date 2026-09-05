package io.heapy.komok.tech.runtime.rootcontext

import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * Root-context sample: providers realize on the framework-owned realm scope whose
 * `CoroutineContext` is `dispatcher + rootContext`. The graph builder's `rootContext`
 * is how the bootstrap installs always-present elements (e.g. the root logging context
 * element), so a provider can rely on them being in `coroutineContext`.
 */
class TenantElement(
    val name: String,
) : AbstractCoroutineContextElement(TenantElement) {
    companion object Key : CoroutineContext.Key<TenantElement>
}

@Module
open class RootContextModule {
    context(_: KomokContext)
    open suspend fun tenant(): String =
        coroutineContext[TenantElement]?.name ?: "absent"
}
