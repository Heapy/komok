package io.heapy.komok.tech.runtime.externalmock

import io.heapy.komok.tech.runtime.GenerateMock
import io.heapy.komok.tech.runtime.test.wrapper.ExternalFormatter
import io.heapy.komok.tech.time.TimeSource

@GenerateMock(ExternalFormatter::class, TimeSource::class)
object ExternalMockRequests
