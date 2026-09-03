package io.heapy.komok.tech.di.delegate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MutableBeanTest {
    @Test
    fun `initializer runs lazily once`() {
        var initializations = 0
        val bean = MutableBean(
            initializer = {
                initializations += 1
                "value"
            },
            name = "test.bean",
        )

        assertFalse(bean.isInitialized)
        assertEquals("value", bean.value)
        assertEquals("value", bean.value)
        assertEquals(1, initializations)
        assertTrue(bean.isInitialized)
    }

    @Test
    fun `mock prevents initializer from running`() {
        var initializations = 0
        val bean = MutableBean(
            initializer = {
                initializations += 1
                "value"
            },
            name = "test.bean",
        )

        val mocked = bean.mock {
            "mocked"
        }

        assertEquals("mocked", mocked)
        assertEquals("mocked", bean.value)
        assertEquals(0, initializations)
    }

    @Test
    fun `setValue prevents initializer from running`() {
        var initializations = 0
        val bean = MutableBean(
            initializer = {
                initializations += 1
                "value"
            },
            name = "test.bean",
        )

        bean.setValue("set")

        assertEquals("set", bean.value)
        assertEquals(0, initializations)
    }

    @Test
    fun `initializer can retry after failure`() {
        var attempts = 0
        val bean = MutableBean(
            initializer = {
                attempts += 1
                if (attempts == 1) {
                    error("first attempt")
                }
                "value"
            },
            name = "test.bean",
        )

        assertFailsWith<IllegalStateException> {
            bean.value
        }
        assertFalse(bean.isInitialized)
        assertEquals("value", bean.value)
        assertEquals(2, attempts)
    }

    @Test
    fun `initialized bean rejects replacement`() {
        val bean = MutableBean(
            initializer = { "value" },
            name = "test.bean",
        )
        val _ = bean.value

        assertFailsWith<IllegalStateException> {
            bean.setValue("replacement")
        }
        assertFailsWith<IllegalStateException> {
            bean.mock { "replacement" }
        }
    }

    @Test
    fun `bean reading itself is reported`() {
        lateinit var bean: MutableBean<String>
        bean = MutableBean(
            initializer = { bean.value },
            name = "test.bean",
        )

        val exception = assertFailsWith<IllegalStateException> {
            bean.value
        }

        assertTrue(exception.message.orEmpty().contains("reads itself"))
        assertFalse(bean.isInitialized)
    }

    @Test
    fun `cycle between two beans is reported`() {
        lateinit var first: MutableBean<String>
        lateinit var second: MutableBean<String>
        first = MutableBean(
            initializer = { second.value },
            name = "test.first",
        )
        second = MutableBean(
            initializer = { first.value },
            name = "test.second",
        )

        val exception = assertFailsWith<IllegalStateException> {
            first.value
        }

        assertTrue(exception.message.orEmpty().contains("reads itself"))
        assertFalse(first.isInitialized)
        assertFalse(second.isInitialized)
    }
}
