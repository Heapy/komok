package io.heapy.komok.tech.decorators.lib

import java.lang.reflect.Method

/**
 * Interface to implement for a decorator.
 *
 * The Current implementation is naive and will be updated to include information:
 * - about other decorators used
 * - include state that can be shared between before and after calls
 * - include state that can be shared between decorators
 *
 * The Current Method type is suboptimal and will be replaced with MethodHandle.
 *
 * @see io.heapy.komok.tech.decorators.lib.Decorated
 */
interface Decorator {
    /**
     * Called before the decorated method is invoked.
     */
    fun before(
        method: Method,
        args: Array<Any?>,
    )

    /**
     * Called after the decorated method is invoked.
     */
    fun after(
        method: Method,
        args: Array<Any?>,
        result: Any?,
    )
}
