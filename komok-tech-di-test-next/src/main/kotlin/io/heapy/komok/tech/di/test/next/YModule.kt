package io.heapy.komok.tech.di.test.next

import io.heapy.komok.tech.di.lib.Module
import io.heapy.komok.tech.di.test.AModule
import io.heapy.komok.tech.di.test.AService
import io.heapy.komok.tech.di.test.MapS

@Module
open class YModule(
    private val aModule: AModule,
) {
    open val yStr by lazy {
        "Y"
    }

    open val yService by lazy {
        YService(
            yStr,
            aModule.aService,
        )
    }
}

class YService(
    private val yStr: String,
    private val aService: AService,
): MapS {
    override fun map(str: String): String {
        return aService.map(str + yStr)
    }
}
