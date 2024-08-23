package io.heapy.komok

interface TestTimeSourceContext : TimeSourceContext {
    override val timeSource: TestTimeSource
}

fun TestTimeSourceContext(
    testTimeSource: TestTimeSource,
): TestTimeSourceContext {
    return DefaultTestTimeSourceContext(
        timeSource = testTimeSource,
    )
}

@JvmInline
private value class DefaultTestTimeSourceContext(
    override val timeSource: TestTimeSource,
) : TestTimeSourceContext
