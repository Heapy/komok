package io.heapy.komok.tech.runtime.test.wrapper

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import io.heapy.komok.tech.runtime.ksp.wrapper.WrapperPlugin
import io.heapy.komok.tech.runtime.ksp.wrapper.WrapperSite

private const val PKG = "io.heapy.komok.tech.runtime.test.wrapper"

/**
 * `@Audit` -> `context(_: AuditTrail)` + `auditCall(meta)`. Registered via
 * `META-INF/services`; slots between the built-in `@Metric` (200) and `@Retry` (300).
 */
class AuditWrapperPlugin : WrapperPlugin {
    override val annotationFqn: String = "$PKG.Audit"
    override val order: Int = 250
    override val contextParameter: ClassName = ClassName(PKG, "AuditTrail")

    override fun helperCall(site: WrapperSite): CodeBlock =
        CodeBlock.of("%M(%L)", MemberName(PKG, "auditCall"), site.metaReference)
}
