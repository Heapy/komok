package io.heapy.komok.store

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

object PostgresJdbc

fun <T> PostgresJdbc.toObject(
    deserializer: DeserializationStrategy<T>,
//    resultSet: ResultSet
): T {
    (0..deserializer.descriptor.elementsCount).forEach {
        println(deserializer.descriptor.getElementName(it))
    }

    TODO()
}


fun <T> Map<String, String>.toObject(deserializer: DeserializationStrategy<T>): T {
    val decoder = MapDecoder(this, deserializer.descriptor)
    return deserializer.deserialize(decoder)
}

class MapDecoder(
    private val map: Map<String, String>,
    private val descriptor: SerialDescriptor,
) : AbstractDecoder() {
    private var index = -1

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        index++
        return if (index < descriptor.elementsCount) index else CompositeDecoder.DECODE_DONE
    }

    override fun decodeInt(): Int = map[getCurrentKey()]?.toInt()
        ?: throw SerializationException("Expected Int value")

    override fun decodeLong(): Long =
        map[getCurrentKey()]?.toLong()
            ?: throw SerializationException("Expected Long value")

    override fun decodeString(): String = map[getCurrentKey()]
        ?: throw SerializationException("Expected String value")

    private fun getCurrentKey() = descriptor.getElementName(index)
}


fun main() {
    val pojo = mapOf(
        "id" to "1",
        "name" to "name",
        "description" to "description",
        "createdAt" to "1",
        "updatedAt" to "1",
    ).toObject(Pojo.serializer())
    println(pojo)
}

@Serializable
data class Pojo(
    val id: Int,
    val name: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long,
)
