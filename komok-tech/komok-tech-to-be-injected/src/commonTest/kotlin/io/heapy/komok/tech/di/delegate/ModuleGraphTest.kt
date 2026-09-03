package io.heapy.komok.tech.di.delegate

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

private class PortableModule1

private class PortableModule2(
    val module1: PortableModule1,
)

private class PortableModule3(
    val module1: PortableModule1,
)

private class PortableModule4(
    val module1: PortableModule1,
    val module2: PortableModule2,
    val module3: PortableModule3,
)

private class CycleA(
    val b: CycleB,
)

private class CycleB(
    val a: CycleA,
)

private class SelfCycle(
    val self: SelfCycle,
)

private fun portableGraph(): ModuleGraphBuilder.() -> Unit = {
    module<PortableModule1> {
        PortableModule1()
    }
    module<PortableModule2> {
        PortableModule2(invoke())
    }
    module<PortableModule3> {
        PortableModule3(invoke())
    }
    module<PortableModule4> {
        PortableModule4(
            module1 = invoke(),
            module2 = invoke(),
            module3 = invoke(),
        )
    }
}

class ModuleGraphTest {
    @Test
    fun `buildModule constructs a portable graph`() {
        val module = buildModule<PortableModule4>(portableGraph())

        assertSame(module.module1, module.module2.module1)
        assertSame(module.module1, module.module3.module1)
    }

    @Test
    fun `buildModules returns every cached module`() {
        val modules = buildModules(portableGraph())

        val module4 = modules<PortableModule4>()
        val module1 = modules<PortableModule1>()

        assertSame(module1, module4.module1)
        assertSame(module1, modules<PortableModule1>())
    }

    @Test
    fun `missing module type fails clearly`() {
        val modules = buildModules {
            module<PortableModule1> {
                PortableModule1()
            }
        }

        val exception = assertFailsWith<IllegalStateException> {
            modules<PortableModule4>()
        }

        assertTrue(exception.message.orEmpty().contains("not found in registry"))
    }

    @Test
    fun `dependency cycle is reported on first access`() {
        val exception = assertFailsWith<IllegalStateException> {
            buildModule<CycleA> {
                module<CycleA> {
                    CycleA(invoke())
                }
                module<CycleB> {
                    CycleB(invoke())
                }
            }
        }

        val message = exception.message.orEmpty()
        assertTrue(message.contains("Module dependency cycle"))
        assertTrue(message.contains("CycleA"))
        assertTrue(message.contains("CycleB"))
    }

    @Test
    fun `self dependency is reported on first access`() {
        val exception = assertFailsWith<IllegalStateException> {
            buildModule<SelfCycle> {
                module<SelfCycle> {
                    SelfCycle(invoke())
                }
            }
        }

        assertTrue(exception.message.orEmpty().contains("Module dependency cycle"))
    }

    @Test
    fun `unused factory does not run`() {
        var built = false

        val modules = buildModules {
            module<PortableModule1> {
                PortableModule1()
            }
            module<PortableModule2> {
                built = true
                PortableModule2(invoke())
            }
        }

        val _ = modules<PortableModule1>()

        assertFalse(built)
    }

    @Test
    fun `duplicate module factory is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            buildModules {
                module<PortableModule1> {
                    PortableModule1()
                }
                module<PortableModule1> {
                    PortableModule1()
                }
            }
        }
    }
}
