package io.heapy.komok.tech.runtime

import kotlinx.coroutines.Deferred

/**
 * Memoization cell state machine for a single binding within one [Realm].
 *
 * Absence of a cell == `Empty`. The first reader CASes `Empty -> InProgress`; the
 * computing coroutine (running on realm-scope) transitions it to [Completed] or
 * [Failed]. Failure is terminal and re-thrown to all readers.
 */
internal sealed interface Cell {
    data class InProgress(val deferred: Deferred<Any?>) : Cell
    data class Completed(val value: Any?) : Cell
    data class Failed(val error: Throwable) : Cell
}
