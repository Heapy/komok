package io.heapy.komok

import java.time.Clock

interface TimeContext {
    val clock: Clock
}
