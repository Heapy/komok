package io.heapy.komok.infra.startup_tasks

import io.heapy.komok.business.register.auto.AutoRegisterStartupTaskModule
import io.heapy.kotomi.runtime.KotomiContext
import io.heapy.kotomi.runtime.Module
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

@Module
class StartupTaskModule(
    private val autoRegisterStartupTaskModule: AutoRegisterStartupTaskModule,
) {
    context(_: KotomiContext)
    suspend fun startupTasks(): List<StartupTask> =
        listOf(
            autoRegisterStartupTaskModule.autoRegisterStartupTask(),
        )

    context(_: KotomiContext)
    suspend fun applicationJob(): Job =
        SupervisorJob()

    context(_: KotomiContext)
    suspend fun startupTaskExecutor(): StartupTaskExecutor =
        StartupTaskExecutor(
            startupTasks = startupTasks(),
            applicationJob = applicationJob(),
        )
}
