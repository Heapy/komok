package io.heapy.komok.tech.di.test

import io.heapy.komok.tech.di.lib.Module

@Module
open class CModule(
    private val bModule: BModule
) {
    open val cList by lazy {
        listOf(
            "C1",
            "C2",
            "C3"
        )
    }

    open val cService by lazy {
        CService(
            cList,
            bModule.bService,
        )
    }
}

class CService(
    private val cList: List<String>,
    private val bService: BService,
) : MapS {
    override fun map(str: String): String {
        return bService.map(str) + cList.joinToString()
    }
}
