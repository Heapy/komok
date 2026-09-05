package io.heapy.komok.tech.runtime.audit

import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module
import io.heapy.komok.tech.runtime.test.wrapper.Audit
import io.heapy.komok.tech.runtime.test.wrapper.AuditTrail

/**
 * WrapperPlugin SPI sample: `@Audit` is a "third-party" wrapper the core processor has
 * never heard of — it is discovered via ServiceLoader from `komok-tech-runtime-test-wrapper`
 * on the ksp classpath. Same user model as built-ins: the wrapped method declares the
 * wrapper's context parameter, the caller provides it.
 */

interface AccountService {
    @Audit
    context(_: AuditTrail)
    suspend fun open(name: String): String
}

class RealAccountService : AccountService {
    context(_: AuditTrail)
    override suspend fun open(name: String): String = "account:$name"
}

@Module
open class AuditModule {
    context(_: KomokContext)
    open suspend fun accountService(): AccountService =
        RealAccountService()
}
