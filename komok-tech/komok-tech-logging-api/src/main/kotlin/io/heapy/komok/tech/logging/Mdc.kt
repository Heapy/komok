package io.heapy.komok.tech.logging

/**
 * Type-safe, immutable diagnostic context (MDC).
 *
 * Inspired by [kotlin.coroutines.CoroutineContext.Key] — each entry has a
 * typed key, giving compile-time safety when reading values.
 *
 * ```
 * object RequestId : Mdc.Key<String>("requestId")
 * val mdc = Mdc.of(RequestId, "abc-123")
 * val rid: String? = mdc[RequestId] // typed!
 * ```
 */
sealed interface Mdc {

    /**
     * Typed key for an MDC entry.
     * Declare as an `object` for singleton keys.
     *
     * @param name Logical name used in encoders, JSON output, log lines, etc.
     */
    abstract class Key<T : Any>(val name: String) {
        override fun toString(): String = "Mdc.Key($name)"
    }

    operator fun <T : Any> get(key: Key<T>): T?

    fun <T : Any> with(key: Key<T>, value: T): Mdc

    operator fun plus(other: Mdc): Mdc

    fun <T : Any> minus(key: Key<T>): Mdc

    fun entries(): Map<String, Any>

    val size: Int

    val isEmpty: Boolean get() = size == 0

    companion object {
        val Empty: Mdc = MapMdc(emptyMap())

        fun <T : Any> of(key: Key<T>, value: T): Mdc = MapMdc(mapOf(key to value))

        fun of(vararg entries: Pair<Key<*>, Any>): Mdc =
            MapMdc(entries.toMap())
    }
}

@PublishedApi
internal class MapMdc(
    @PublishedApi internal val map: Map<Mdc.Key<*>, Any>,
) : Mdc {

    override val size: Int get() = map.size

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: Mdc.Key<T>): T? = map[key] as? T

    override fun <T : Any> with(key: Mdc.Key<T>, value: T): Mdc =
        MapMdc(map + (key to value))

    override fun plus(other: Mdc): Mdc = when {
        other.isEmpty -> this
        this.isEmpty -> other
        other is MapMdc -> MapMdc(this.map + other.map)
        else -> this
    }

    override fun <T : Any> minus(key: Mdc.Key<T>): Mdc = MapMdc(map - key)

    override fun entries(): Map<String, Any> =
        map.entries.associate { [k, v] -> k.name to v }

    override fun toString(): String =
        map.entries.joinToString(prefix = "Mdc{", postfix = "}") { [k, v] -> "${k.name}=$v" }

    override fun equals(other: Any?): Boolean = other is MapMdc && map == other.map
    override fun hashCode(): Int = map.hashCode()
}

inline fun buildMdc(block: MdcBuilder.() -> Unit): Mdc {
    val builder = MdcBuilder()
    builder.block()
    return builder.build()
}

class MdcBuilder @PublishedApi internal constructor() {
    private val entries = mutableMapOf<Mdc.Key<*>, Any>()

    operator fun <T : Any> Mdc.Key<T>.invoke(value: T) {
        entries[this] = value
    }

    infix fun <T : Any> Mdc.Key<T>.to(value: T) {
        entries[this] = value
    }

    @PublishedApi
    internal fun build(): Mdc = MapMdc(entries.toMap())
}
