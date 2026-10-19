package io.heapy.komok.infra.startup_tasks

import io.heapy.komok.business.register.auto.AutoRegisterStartupTaskModule
import io.heapy.komok.tech.runtime.KomokContext
import io.heapy.komok.tech.runtime.Module
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

@Module
class StartupTaskModule(
    private val autoRegisterStartupTaskModule: AutoRegisterStartupTaskModule,
) {
    context(_: KomokContext)
    suspend fun startupTasks(): List<StartupTask> =
        listOf(
            autoRegisterStartupTaskModule.autoRegisterStartupTask(),
        )

    context(_: KomokContext)
    suspend fun applicationJob(): Job =
        SupervisorJob()

    context(_: KomokContext)
    suspend fun startupTaskExecutor(): StartupTaskExecutor =
        StartupTaskExecutor(
            startupTasks = startupTasks(),
            applicationJob = applicationJob(),
        )
}
