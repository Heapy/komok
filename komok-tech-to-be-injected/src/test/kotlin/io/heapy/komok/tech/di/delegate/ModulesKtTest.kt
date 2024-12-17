package io.heapy.komok.tech.di.delegate

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class Bean1

private class BuildModule1 {
    val bean1 by bean {
        Bean1()
    }
}
private class BuildModule2(
    val module1: BuildModule1,
)
private class BuildModule3(
    val module1: BuildModule1,
)
private class BuildModule4(
    val module1: BuildModule1,
    val module2: BuildModule2,
    val module3: BuildModule3,
)

class ModulesKtTest {
    @Test
    fun `simple modules`() {
        val module1 = buildModule<BuildModule1>()
        val module2 = buildModule<BuildModule2>()
        val module3 = buildModule<BuildModule3>()
        val module4 = buildModule<BuildModule4>()

        assertNotNull(module1)
        assertNotNull(module2)
        assertNotNull(module3)
        assertNotNull(module4)
    }

    @Test
    fun `cached modules`() {
        val module = buildModule<BuildModule4>()

        assertSame(module.module1, module.module3.module1)
        assertSame(module.module1, module.module2.module1)
        assertSame(module.module1.bean1, module.module3.module1.bean1)
        assertSame(module.module1.bean1, module.module2.module1.bean1)
        assertSame(module.module1.bean1.value, module.module3.module1.bean1.value)
        assertSame(module.module1.bean1.value, module.module2.module1.bean1.value)
    }
}
