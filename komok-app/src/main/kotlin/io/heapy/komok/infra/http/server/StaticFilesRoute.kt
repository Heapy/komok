package io.heapy.komok.infra.http.server

import io.heapy.komok.server.common.KomokRoute
import io.ktor.server.http.content.CompressedFileType
import io.ktor.server.http.content.staticFiles
import io.ktor.server.routing.*
import java.io.File

class StaticFilesRoute(
    private val resources: String,
) : KomokRoute {
    override fun Routing.install() {
        staticFiles(
            "/",
            File(resources)
        ) {
            preCompressed(
                CompressedFileType.BROTLI,
                CompressedFileType.GZIP
            )
            enableAutoHeadResponse()
        }
    }
}
