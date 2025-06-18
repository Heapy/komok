package io.heapy.komok.tech.di.delegate

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

private class Module1 {
    val bean1 by bean {
        "bean1"
    }
}

private class Module2(
    val module1: Module1,
) {
    val bean2 by bean {
        "bean2" + module1.bean1.value
    }
}

class BeanKtTest {
    @Test
    fun `simple beans`() {
        val module1 = Module1()
        val module2 = Module2(module1)

        assertEquals("bean1", module1.bean1.value)
        assertEquals("bean2bean1", module2.bean2.value)
    }

    @Test
    fun `override dependent bean`() {
        val module1 = Module1()
        val module2 = Module2(module1)

        module1.bean1.setValue("bean1new")

        assertEquals("bean1new", module1.bean1.value)
        assertEquals("bean2bean1new", module2.bean2.value)
    }

    @Test
    fun `override root bean`() {
        val module1 = Module1()
        val module2 = Module2(module1)

        module2.bean2.setValue("bean2new")

        assertEquals("bean1", module1.bean1.value)
        assertEquals("bean2new", module2.bean2.value)
    }
}
