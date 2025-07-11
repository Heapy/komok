package io.heapy.komok.infra.startup_tasks

import io.heapy.komok.business.register.auto.AutoRegisterStartupTaskModule
import io.heapy.komok.tech.di.lib.Module
import kotlinx.coroutines.SupervisorJob

@Module
open class StartupTaskModule(
    private val autoRegisterStartupTaskModule: AutoRegisterStartupTaskModule,
) {
    open val startupTasks by lazy {
        listOf(
            autoRegisterStartupTaskModule.autoRegisterStartupTask,
        )
    }

    open val applicationJob by lazy {
        SupervisorJob()
    }

    open val startupTaskExecutor by lazy {
        StartupTaskExecutor(
            startupTasks = startupTasks,
            applicationJob = applicationJob,
        )
    }
}
