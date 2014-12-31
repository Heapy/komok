//package io.heapy.vipassana.http
//
//import io.netty.buffer.Unpooled
//import io.netty.channel.ChannelFuture
//import io.netty.channel.ChannelFutureListener
//import io.netty.channel.ChannelHandlerContext
//import io.netty.channel.ChannelProgressiveFuture
//import io.netty.channel.ChannelProgressiveFutureListener
//import io.netty.channel.DefaultFileRegion
//import io.netty.channel.SimpleChannelInboundHandler
//import io.netty.handler.codec.http.DefaultFullHttpResponse
//import io.netty.handler.codec.http.DefaultHttpResponse
//import io.netty.handler.codec.http.FullHttpRequest
//import io.netty.handler.codec.http.FullHttpResponse
//import io.netty.handler.codec.http.HttpChunkedInput
//import io.netty.handler.codec.http.HttpHeaderNames
//import io.netty.handler.codec.http.HttpHeaderValues
//import io.netty.handler.codec.http.HttpMethod
//import io.netty.handler.codec.http.HttpResponse
//import io.netty.handler.codec.http.HttpResponseStatus
//import io.netty.handler.codec.http.HttpUtil
//import io.netty.handler.codec.http.HttpVersion
//import io.netty.handler.codec.http.LastHttpContent
//import io.netty.handler.ssl.SslHandler
//import io.netty.handler.stream.ChunkedFile
//import io.netty.util.CharsetUtil
//import io.netty.util.internal.SystemPropertyUtil
//import jakarta.activation.MimetypesFileTypeMap
//import java.io.File
//import java.io.FileNotFoundException
//import java.io.RandomAccessFile
//import java.io.UnsupportedEncodingException
//import java.net.URLDecoder
//import java.text.SimpleDateFormat
//import java.util.Calendar
//import java.util.Date
//import java.util.GregorianCalendar
//import java.util.Locale
//import java.util.TimeZone
//import java.util.regex.Pattern
//
///**
// * A simple handler that serves incoming HTTP requests to send their respective
// * HTTP responses.  It also implements `'If-Modified-Since'` header to
// * take advantage of browser cache, as described in
// * [RFC 2616](https://tools.ietf.org/html/rfc2616#section-14.25).
// *
// * <h3>How Browser Caching Works</h3>
// *
// * Web browser caching works with HTTP headers as illustrated by the following
// * sample:
// *
// *  1. Request #1 returns the content of `/file1.txt`.
// *  1. Contents of `/file1.txt` is cached by the browser.
// *  1. Request #2 for `/file1.txt` does not return the contents of the
// * file again. Rather, a 304 Not Modified is returned. This tells the
// * browser to use the contents stored in its cache.
// *  1. The server knows the file has not been modified because the
// * `If-Modified-Since` date is the same as the file's last
// * modified date.
// *
// *
// * <pre>
// * Request #1 Headers
// * ===================
// * GET /file1.txt HTTP/1.1
// *
// * Response #1 Headers
// * ===================
// * HTTP/1.1 200 OK
// * Date:               Tue, 01 Mar 2011 22:44:26 GMT
// * Last-Modified:      Wed, 30 Jun 2010 21:36:48 GMT
// * Expires:            Tue, 01 Mar 2012 22:44:26 GMT
// * Cache-Control:      private, max-age=31536000
// *
// * Request #2 Headers
// * ===================
// * GET /file1.txt HTTP/1.1
// * If-Modified-Since:  Wed, 30 Jun 2010 21:36:48 GMT
// *
// * Response #2 Headers
// * ===================
// * HTTP/1.1 304 Not Modified
// * Date:               Tue, 01 Mar 2011 22:44:28 GMT
// *
//</pre> *
// */
//internal class HttpStaticFileServerHandler : SimpleChannelInboundHandler<FullHttpRequest>() {
//    private var request: FullHttpRequest? = null
//
//    @Throws(Exception::class)
//    public override fun channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest) {
//        this.request = request
//        if (!request.decoderResult().isSuccess) {
//            sendError(ctx, HttpResponseStatus.BAD_REQUEST)
//            return
//        }
//        if (HttpMethod.GET != request.method()) {
//            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED)
//            return
//        }
//        val keepAlive = HttpUtil.isKeepAlive(request)
//        val uri = request.uri()
//        val path = sanitizeUri(uri)
//        if (path == null) {
//            sendError(ctx, HttpResponseStatus.FORBIDDEN)
//            return
//        }
//        val file = File(path)
//        if (file.isHidden || !file.exists()) {
//            sendError(ctx, HttpResponseStatus.NOT_FOUND)
//            return
//        }
//        if (file.isDirectory) {
//            if (uri.endsWith("/")) {
//                sendListing(ctx, file, uri)
//            } else {
//                sendRedirect(ctx, "$uri/")
//            }
//            return
//        }
//        if (!file.isFile) {
//            sendError(ctx, HttpResponseStatus.FORBIDDEN)
//            return
//        }
//
//        // Cache Validation
//        val ifModifiedSince = request.headers()[HttpHeaderNames.IF_MODIFIED_SINCE]
//        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
//            val dateFormatter = SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US)
//            val ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince)
//
//            // Only compare up to the second because the datetime format we send to the client
//            // does not have milliseconds
//            val ifModifiedSinceDateSeconds = ifModifiedSinceDate.time / 1000
//            val fileLastModifiedSeconds = file.lastModified() / 1000
//            if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
//                sendNotModified(ctx)
//                return
//            }
//        }
//        val raf: RandomAccessFile
//        raf = try {
//            RandomAccessFile(file, "r")
//        } catch (ignore: FileNotFoundException) {
//            sendError(ctx, HttpResponseStatus.NOT_FOUND)
//            return
//        }
//        val fileLength = raf.length()
//        val response: HttpResponse = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
//        HttpUtil.setContentLength(response, fileLength)
//        setContentTypeHeader(response, file)
//        setDateAndCacheHeaders(response, file)
//        if (!keepAlive) {
//            response.headers()[HttpHeaderNames.CONNECTION] = HttpHeaderValues.CLOSE
//        } else if (request.protocolVersion() == HttpVersion.HTTP_1_0) {
//            response.headers()[HttpHeaderNames.CONNECTION] = HttpHeaderValues.KEEP_ALIVE
//        }
//
//        // Write the initial line and the header.
//        ctx.write(response)
//
//        // Write the content.
//        val sendFileFuture: ChannelFuture
//        val lastContentFuture: ChannelFuture
//        if (ctx.pipeline().get(SslHandler::class.java) == null) {
//            sendFileFuture = ctx.write(DefaultFileRegion(raf.channel, 0, fileLength), ctx.newProgressivePromise())
//            // Write the end marker.
//            lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
//        } else {
//            sendFileFuture = ctx.writeAndFlush(HttpChunkedInput(ChunkedFile(raf, 0, fileLength, 8192)),
//                ctx.newProgressivePromise())
//            // HttpChunkedInput will write the end marker (LastHttpContent) for us.
//            lastContentFuture = sendFileFuture
//        }
//        sendFileFuture.addListener(object : ChannelProgressiveFutureListener {
//            override fun operationProgressed(future: ChannelProgressiveFuture, progress: Long, total: Long) {
//                if (total < 0) { // total unknown
//                    System.err.println(future.channel().toString() + " Transfer progress: " + progress)
//                } else {
//                    System.err.println(future.channel().toString() + " Transfer progress: " + progress + " / " + total)
//                }
//            }
//
//            override fun operationComplete(future: ChannelProgressiveFuture) {
//                System.err.println(future.channel().toString() + " Transfer complete.")
//            }
//        })
//
//        // Decide whether to close the connection or not.
//        if (!keepAlive) {
//            // Close the connection when the whole content is written out.
//            lastContentFuture.addListener(ChannelFutureListener.CLOSE)
//        }
//    }
//
//    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
//        cause.printStackTrace()
//        if (ctx.channel().isActive) {
//            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR)
//        }
//    }
//
//    private fun sendListing(ctx: ChannelHandlerContext, dir: File, dirPath: String) {
//        val buf = StringBuilder()
//            .append("<!DOCTYPE html>\r\n")
//            .append("<html><head><meta charset='utf-8' /><title>")
//            .append("Listing of: ")
//            .append(dirPath)
//            .append("</title></head><body>\r\n")
//            .append("<h3>Listing of: ")
//            .append(dirPath)
//            .append("</h3>\r\n")
//            .append("<ul>")
//            .append("<li><a href=\"../\">..</a></li>\r\n")
//        val files = dir.listFiles()
//        if (files != null) {
//            for (f in files) {
//                if (f.isHidden || !f.canRead()) {
//                    continue
//                }
//                val name = f.name
//                if (!ALLOWED_FILE_NAME.matcher(name).matches()) {
//                    continue
//                }
//                buf.append("<li><a href=\"")
//                    .append(name)
//                    .append("\">")
//                    .append(name)
//                    .append("</a></li>\r\n")
//            }
//        }
//        buf.append("</ul></body></html>\r\n")
//        val buffer = ctx.alloc().buffer(buf.length)
//        buffer.writeCharSequence(buf.toString(), CharsetUtil.UTF_8)
//        val response: FullHttpResponse = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer)
//        response.headers()[HttpHeaderNames.CONTENT_TYPE] = "text/html; charset=UTF-8"
//        sendAndCleanupConnection(ctx, response)
//    }
//
//    private fun sendRedirect(ctx: ChannelHandlerContext, newUri: String) {
//        val response: FullHttpResponse = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND, Unpooled.EMPTY_BUFFER)
//        response.headers()[HttpHeaderNames.LOCATION] = newUri
//        sendAndCleanupConnection(ctx, response)
//    }
//
//    private fun sendError(ctx: ChannelHandlerContext, status: HttpResponseStatus) {
//        val response: FullHttpResponse = DefaultFullHttpResponse(
//            HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: $status\r\n", CharsetUtil.UTF_8))
//        response.headers()[HttpHeaderNames.CONTENT_TYPE] = "text/plain; charset=UTF-8"
//        sendAndCleanupConnection(ctx, response)
//    }
//
//    /**
//     * When file timestamp is the same as what the browser is sending up, send a "304 Not Modified"
//     *
//     * @param ctx
//     * Context
//     */
//    private fun sendNotModified(ctx: ChannelHandlerContext) {
//        val response: FullHttpResponse = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_MODIFIED, Unpooled.EMPTY_BUFFER)
//        setDateHeader(response)
//        sendAndCleanupConnection(ctx, response)
//    }
//
//    /**
//     * If Keep-Alive is disabled, attaches "Connection: close" header to the response
//     * and closes the connection after the response being sent.
//     */
//    private fun sendAndCleanupConnection(ctx: ChannelHandlerContext, response: FullHttpResponse) {
//        val request = request
//        val keepAlive = HttpUtil.isKeepAlive(request)
//        HttpUtil.setContentLength(response, response.content().readableBytes().toLong())
//        if (!keepAlive) {
//            // We're going to close the connection as soon as the response is sent,
//            // so we should also make it clear for the client.
//            response.headers()[HttpHeaderNames.CONNECTION] = HttpHeaderValues.CLOSE
//        } else if (request!!.protocolVersion() == HttpVersion.HTTP_1_0) {
//            response.headers()[HttpHeaderNames.CONNECTION] = HttpHeaderValues.KEEP_ALIVE
//        }
//        val flushPromise = ctx.writeAndFlush(response)
//        if (!keepAlive) {
//            // Close the connection as soon as the response is sent.
//            flushPromise.addListener(ChannelFutureListener.CLOSE)
//        }
//    }
//
//    companion object {
//        const val HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz"
//        const val HTTP_DATE_GMT_TIMEZONE = "GMT"
//        const val HTTP_CACHE_SECONDS = 60
//        private val INSECURE_URI = Pattern.compile(".*[<>&\"].*")
//        private fun sanitizeUri(uri: String): String? {
//            // Decode the path.
//            var uri = uri
//            uri = try {
//                URLDecoder.decode(uri, "UTF-8")
//            } catch (e: UnsupportedEncodingException) {
//                throw Error(e)
//            }
//            if (uri.isEmpty() || uri[0] != '/') {
//                return null
//            }
//
//            // Convert file separators.
//            uri = uri.replace('/', File.separatorChar)
//
//            // Simplistic dumb security check.
//            // You will have to do something serious in the production environment.
//            return if (uri.contains(File.separator + '.') ||
//                uri.contains('.'.toString() + File.separator) || uri[0] == '.' || uri[uri.length - 1] == '.' ||
//                INSECURE_URI.matcher(uri).matches()) {
//                null
//            } else SystemPropertyUtil.get("user.dir") + File.separator + uri
//
//            // Convert to absolute path.
//        }
//
//        private val ALLOWED_FILE_NAME = Pattern.compile("[^-\\._]?[^<>&\\\"]*")
//
//        /**
//         * Sets the Date header for the HTTP response
//         *
//         * @param response
//         * HTTP response
//         */
//        private fun setDateHeader(response: FullHttpResponse) {
//            val dateFormatter = SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US)
//            dateFormatter.timeZone = TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE)
//            val time: Calendar = GregorianCalendar()
//            response.headers()[HttpHeaderNames.DATE] = dateFormatter.format(time.time)
//        }
//
//        /**
//         * Sets the Date and Cache headers for the HTTP Response
//         *
//         * @param response
//         * HTTP response
//         * @param fileToCache
//         * file to extract content type
//         */
//        private fun setDateAndCacheHeaders(response: HttpResponse, fileToCache: File) {
//            val dateFormatter = SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US)
//            dateFormatter.timeZone = TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE)
//
//            // Date header
//            val time: Calendar = GregorianCalendar()
//            response.headers()[HttpHeaderNames.DATE] = dateFormatter.format(time.time)
//
//            // Add cache headers
//            time.add(Calendar.SECOND, HTTP_CACHE_SECONDS)
//            response.headers()[HttpHeaderNames.EXPIRES] = dateFormatter.format(time.time)
//            response.headers()[HttpHeaderNames.CACHE_CONTROL] = "private, max-age=" + HTTP_CACHE_SECONDS
//            response.headers()[HttpHeaderNames.LAST_MODIFIED] = dateFormatter.format(Date(fileToCache.lastModified()))
//        }
//
//        /**
//         * Sets the content type header for the HTTP Response
//         *
//         * @param response
//         * HTTP response
//         * @param file
//         * file to extract content type
//         */
//        private fun setContentTypeHeader(response: HttpResponse, file: File) {
//            val mimeTypesMap = MimetypesFileTypeMap()
//            response.headers()[HttpHeaderNames.CONTENT_TYPE] = mimeTypesMap.getContentType(file.path)
//        }
//    }
//}
