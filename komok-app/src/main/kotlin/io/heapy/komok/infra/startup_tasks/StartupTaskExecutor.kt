package io.heapy.komok.infra.startup_tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class StartupTaskExecutor(
    private val applicationJob: Job,
    private val startupTasks: List<StartupTask>,
) {
    fun executeAll() {
        startupTasks.forEach { task ->
            CoroutineScope(applicationJob).launch(Dispatchers.IO) {
                task.execute()
            }
        }
    }
}
