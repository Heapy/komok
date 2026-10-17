package io.heapy.komok.tech.di.test

import io.heapy.komok.tech.di.lib.Module

@Module
open class AModule(
    bModule: BModule,
) {
    open val aStr by lazy {
        "A"
    }

    open val aService by lazy {
        AService(
            aStr,
            bModule.bService,
        )
    }
}

class AService(
    private val aStr: String,
    private val mapS: MapS,
): MapS {
    override fun map(str: String): String {
        return mapS.map(str + aStr)
    }
}

