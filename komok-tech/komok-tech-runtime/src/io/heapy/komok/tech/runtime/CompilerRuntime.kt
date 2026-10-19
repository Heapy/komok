package io.heapy.komok.tech.runtime

/** Runtime entry point emitted into provider bodies by the Komok compiler plugin. */
context(context: KomokContext)
suspend fun <T> komokBinding(
    owner: String,
    name: String,
    parameters: String,
    body: suspend context(KomokContext) () -> T,
    decorate: (T) -> T = { it },
): T {
    val id = BindingId(owner, ProviderSignature(name, parameters))
    return if (context.bypassBinding == id) {
        decorate(body(KomokContext(context.realm, context.callerStack)))
    } else {
        context.realm.runtime.binding(id) { decorate(body()) }
    }
}
