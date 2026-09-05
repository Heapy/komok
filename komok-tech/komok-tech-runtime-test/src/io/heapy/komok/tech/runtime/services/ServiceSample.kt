package io.heapy.komok.tech.runtime.services

import io.heapy.komok.tech.runtime.IntoSet
import io.heapy.komok.tech.runtime.ProvidersFrom
import io.heapy.komok.tech.runtime.Service
import io.heapy.komok.tech.runtime.Shutdown
import io.heapy.komok.tech.runtime.Startup
import io.heapy.komok.tech.runtime.interfaces.InterfaceCodec
import io.heapy.komok.tech.runtime.interfaces.InterfaceNamedCodec
import io.heapy.komok.tech.time.IndexedTimeZone
import io.heapy.komok.tech.time.LegacyTimeProviders
import io.heapy.komok.tech.time.ProvidersFromTimeZone
import io.heapy.komok.tech.time.TimeCodec
import java.util.concurrent.CopyOnWriteArrayList

data class ServiceConfig(val value: String)

data class FunctionRepository(
    val config: ServiceConfig,
    val source: String,
)

class ClassBackedService(
    val repository: FunctionRepository,
) {
    fun value(): String =
        "${repository.source}:${repository.config.value}"
}

@Service
fun serviceConfig(): ServiceConfig =
    ServiceConfig("root")

@Service
fun functionRepository(config: ServiceConfig): FunctionRepository =
    FunctionRepository(config = config, source = "function")

@Service
class ClassBackedServiceProvider(
    val repository: FunctionRepository,
) {
    fun value(): String =
        "class:${repository.config.value}"
}

interface PaymentGateway {
    fun charge(): String
}

@Service(bind = PaymentGateway::class)
class RealPaymentGateway : PaymentGateway {
    override fun charge(): String =
        "charged"
}

interface PaymentSettings {
    val mode: String
}

class DefaultPaymentSettings : PaymentSettings {
    override val mode: String = "live"
}

@Service(PaymentSettings::class)
fun paymentSettings(): DefaultPaymentSettings =
    DefaultPaymentSettings()

@Service
class PaymentClient(
    private val gateway: PaymentGateway,
    private val settings: PaymentSettings,
) {
    fun checkout(): String =
        "${gateway.charge()}:${settings.mode}"
}

interface Codec {
    val name: String
}

data class NamedCodec(
    override val name: String,
) : Codec

@Service
@IntoSet
fun jsonCodec(): Codec =
    NamedCodec("json")

@Service
@IntoSet
fun xmlCodec(): Codec =
    NamedCodec("xml")

@Service
@IntoSet
fun serviceInterfaceCodec(): InterfaceCodec =
    InterfaceNamedCodec("service")

@Service
class CodecRegistry(
    val codecs: Set<Codec>,
) {
    fun names(): List<String> =
        codecs.map { it.name }
}

@Service
class IndexedTimeZoneConsumer(
    private val indexedTimeZone: IndexedTimeZone,
) {
    fun zoneId(): String =
        indexedTimeZone.id
}

@ProvidersFrom(LegacyTimeProviders::class)
object ServiceProvidersFrom

@Service
class ProvidersFromTimeZoneConsumer(
    private val providersFromTimeZone: ProvidersFromTimeZone,
) {
    fun zoneId(): String =
        providersFromTimeZone.id
}

@Service
class TimeCodecRegistry(
    val codecs: Set<TimeCodec>,
) {
    fun names(): List<String> =
        codecs.map { it.name }
}

@Service
fun stringItems(): List<String> =
    listOf("alpha", "beta")

@Service
fun intItems(): List<Int> =
    listOf(10, 20)

@Service
class GenericStringItemsConsumer(
    private val items: List<String>,
) {
    fun joined(): String =
        items.joinToString(",")
}

@Service
class GenericIntItemsConsumer(
    private val items: List<Int>,
) {
    fun total(): Int =
        items.sum()
}

class LifecycleEvents(
    val entries: MutableList<String> = CopyOnWriteArrayList(),
)

@Service
fun lifecycleEvents(): LifecycleEvents =
    LifecycleEvents()

@Service
class LifecycleDependency(
    private val events: LifecycleEvents,
) {
    fun marker(): String =
        "dependency"

    @Startup
    suspend fun start() {
        events.entries.add("dependency.start")
    }

    @Shutdown
    suspend fun stop() {
        events.entries.add("dependency.stop")
    }
}

@Service
class LifecycleDependent(
    private val dependency: LifecycleDependency,
    private val events: LifecycleEvents,
) {
    @Startup
    suspend fun start() {
        check(dependency.marker() == "dependency")
        events.entries.add("dependent.start")
    }

    @Shutdown
    suspend fun stop() {
        events.entries.add("dependent.stop")
    }
}
