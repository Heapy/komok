package io.heapy.komok.infra.loom

import io.heapy.komok.infra.di.module
import io.heapy.komok.infra.di.provide
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

class CoroutinesLoomDispatcher : ExecutorCoroutineDispatcher() {
    private val factory = Thread.ofVirtual().name("coroutines-virtual-", 0).factory()
    override val executor: Executor = Executors.newThreadPerTaskExecutor(factory)
    private val dispatcher = executor.asCoroutineDispatcher()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatcher.dispatch(context, block)
    }

    override fun close() {
        error("Cannot be invoked on CoroutinesLoomDispatcher")
    }
}

val dispatcherModule by module {
    provide(::CoroutinesLoomDispatcher)
}
