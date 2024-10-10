package io.heapy.komok.business

import io.heapy.komok.server.common.KomokServerFeature
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlin.time.Duration.Companion.seconds

class WebSocketsFeature : KomokServerFeature {
    override fun Application.install() {
        install(WebSockets) {
            pingPeriod = 15.seconds
            timeout = 15.seconds
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

        routing {
            webSocket("/ws") {
                println("new session is open")
                for (frame in incoming) {
                    frame.fin
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            outgoing.send(Frame.Text("YOU SAID: $text"))
                            if (text.equals(
                                    "bye",
                                    ignoreCase = true,
                                )
                            ) {
                                close(
                                    CloseReason(
                                        CloseReason.Codes.NORMAL,
                                        "Client said BYE",
                                    ),
                                )
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}
