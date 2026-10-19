package io.heapy.komok.tech.runtime

/** Intrinsic: generates an implementation only for this explicitly requested interface. */
fun <T : Any> mock(): Mock<T> = compilerRequired()

/** Intrinsic: unstubbed calls delegate to [real]. */
fun <T : Any> spy(real: T): Mock<T> = compilerRequired()

/** A mock value and its typed configuration API. Selectors identify a method, never execute it. */
class Mock<T : Any>(val value: T, val controller: MockController) {
    fun <R> every(selector: suspend T.() -> R): MockStub<R> = compilerRequired()

    fun <R> calls(selector: suspend T.() -> R): List<MockCall> = compilerRequired()

    fun <R> stub(method: String): MockStub<R> = MockStub(controller, method)

    fun recordedCalls(method: String): List<MockCall> = controller.calls(method)
}

data class MockCall(val method: String, val arguments: List<Any?>)

class MockStub<R>(private val controller: MockController, private val method: String) {
    infix fun returns(value: R) {
        answers { value }
    }

    infix fun answers(answer: (MockCall) -> R) {
        controller.answer(method, answer)
    }

    infix fun coAnswers(answer: suspend (MockCall) -> R) {
        controller.suspendAnswer(method, answer)
    }
}

/** Shared dispatch for generated interface implementations; contains no reflection. */
class MockController {
    private class Answer(
        val regular: ((MockCall) -> Any?)?,
        val suspending: (suspend (MockCall) -> Any?)?,
    )

    private val answers = KomokConcurrentMap<String, Answer>()
    private val recorded = KomokConcurrentList<MockCall>()

    fun answer(method: String, answer: (MockCall) -> Any?) {
        answers.put(method, Answer(answer, null))
    }

    fun suspendAnswer(method: String, answer: suspend (MockCall) -> Any?) {
        answers.put(method, Answer(null, answer))
    }

    fun calls(method: String): List<MockCall> = recorded.snapshot().filter { it.method == method }

    fun call(method: String, arguments: List<Any?>, delegate: (() -> Any?)?): Any? {
        val call = MockCall(method, arguments)
        realized(recorded.add(call))
        val answer = answers[method]
        if (answer != null) return checkNotNull(answer.regular) { "Suspend answer registered for non-suspend method $method" }(call)
        return (delegate ?: error("No stub for $method"))()
    }

    suspend fun suspendCall(method: String, arguments: List<Any?>, delegate: (suspend () -> Any?)?): Any? {
        val call = MockCall(method, arguments)
        realized(recorded.add(call))
        val answer = answers[method]
        if (answer != null) return if (answer.suspending != null) answer.suspending(call)
            else checkNotNull(answer.regular)(call)
        return (delegate ?: error("No stub for $method"))()
    }
}
