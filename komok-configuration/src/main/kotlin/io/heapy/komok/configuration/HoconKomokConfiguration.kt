package io.heapy.komok.configuration

import com.typesafe.config.Config
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.Hocon.Default.decodeFromConfig

interface KomokConfiguration {
    fun <T : Any> read(
        deserializer: DeserializationStrategy<T>,
        path: String,
    ): T
}

class MockKomokConfigurationBuilder {
    private val config = mutableMapOf<DeserializationStrategy<*>, Any>()

    fun <T : Any> add(
        deserializer: DeserializationStrategy<T>,
        value: T,
    ) {
        config[deserializer] = value
    }

    internal fun build(): KomokConfiguration {
        return MockKomokConfiguration(config)
    }
}

fun buildMockKomokConfiguration(
    builder: MockKomokConfigurationBuilder.() -> Unit,
): KomokConfiguration {
    val mockKomokConfigurationBuilder = MockKomokConfigurationBuilder()
    mockKomokConfigurationBuilder.builder()
    return mockKomokConfigurationBuilder.build()
}

internal data class MockKomokConfiguration(
    private val config: Map<DeserializationStrategy<*>, Any>,
) : KomokConfiguration {
    override fun <T : Any> read(
        deserializer: DeserializationStrategy<T>,
        path: String,
    ): T {
        @Suppress("UNCHECKED_CAST")
        return config[deserializer] as T
    }
}

internal class HoconKomokConfiguration(
    private val config: Config,
) : KomokConfiguration {
    @OptIn(ExperimentalSerializationApi::class)
    override fun <T : Any> read(
        deserializer: DeserializationStrategy<T>,
        path: String
    ): T {
        return decodeFromConfig(
            deserializer = deserializer,
            config = config.getConfig(path),
        )
    }
}
