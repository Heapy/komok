package io.heapy.komok.tech.di.test2

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class SingletonTest {
    @Test
    fun `test singleton`() {
        val moduleD = createModuleD {}

        assertAll(
            { assertSame(moduleD.d.getAA(), moduleD.d.getAB()) },
            { assertSame(moduleD.d.getAB(), moduleD.d.getAC()) },
            { assertSame(moduleD.d.getAA(), moduleD.d.getAC()) },
        )
    }

    @Test
    fun `test singleton flatten`() {
        val flat = createFlattenModuleD {}

        assertAll(
            { assertSame(flat.moduleD.d.getAA(), flat.moduleD.d.getAB()) },
            { assertSame(flat.moduleD.d.getAB(), flat.moduleD.d.getAC()) },
            { assertSame(flat.moduleD.d.getAA(), flat.moduleD.d.getAC()) },
            { assertSame(flat.moduleA.a, flat.moduleD.d.getAA()) },
        )
    }

    @Test
    fun `test singleton with override`() {
        val a = A()
        val moduleD = createModuleD {
            moduleA {
                a {
                    a
                }
            }
        }

        assertAll(
            { assertSame(moduleD.d.getAA(), moduleD.d.getAB()) },
            { assertSame(moduleD.d.getAB(), moduleD.d.getAC()) },
            { assertSame(moduleD.d.getAA(), moduleD.d.getAC()) },
        )
    }

    @Test
    fun `test singleton with override flatten`() {
        val a = A()
        val flat = createFlattenModuleD {
            moduleA {
                a {
                    a
                }
            }
        }

        assertAll(
            { assertSame(a, flat.moduleD.d.getAA()) },
            { assertSame(a, flat.moduleD.d.getAB()) },
            { assertSame(a, flat.moduleD.d.getAC()) },
            { assertSame(a, flat.moduleA.a) },
            { assertSame(a, flat.moduleB.b.getA()) },
            { assertSame(a, flat.moduleC.c.getA()) },
        )
    }
}
