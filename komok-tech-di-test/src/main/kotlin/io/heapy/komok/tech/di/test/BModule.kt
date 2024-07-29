package io.heapy.komok.tech.di.test

import io.heapy.komok.tech.di.lib.Module

@Module
open class BModule {
    open val bStr by lazy {
        "B"
    }

    open val bService by lazy {
        BService(
            bStr,
        )
    }
}

class BService(
    private val bStr: String,
) : MapS {
    override fun map(str: String): String {
        return str + bStr
    }
}
