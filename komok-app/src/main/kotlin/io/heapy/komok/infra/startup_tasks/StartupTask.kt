package io.heapy.komok.infra.startup_tasks

interface StartupTask {
    suspend fun execute()
}
