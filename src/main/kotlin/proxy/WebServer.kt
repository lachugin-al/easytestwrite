package proxy

import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.file.Files
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import events.Event
import events.EventData
import events.EventStorage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import utils.NetworkUtils
import java.time.Instant

/**
 * Local HTTP server for the test infrastructure.
 *
 * Provides functionality for:
 * - Hosting local files (/file/)
 * - Receiving client events (/m/batch) and saving them in [EventStorage]
 *
 * It is part of the mobile and web application testing system.
 */
class WebServer : AutoCloseable {
    private val logger: Logger = LoggerFactory.getLogger(WebServer::class.java)

    private var webServer: HttpServer? = null
    private var serverUrl: String? = null
    private var paused: Boolean = false

    init {
        // Register a shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                close()
            } catch (e: Exception) {
                logger.error("Error while closing WebServer in shutdown hook: ${e.message}", e)
            }
        })
    }

    /**
     * Starts the web server.
     *
     * The server starts on the local IP address and a dynamically selected free port.
     */
    fun start() {
        val host = NetworkUtils.getLocalAddress()
        val port = 8000 // NetworkUtils.getFreePort()
        try {
            webServer = HttpServer.create(InetSocketAddress(host, port), 0)
        } catch (e: IOException) {
            throw RuntimeException("Failed to initialize web server", e)
        }

        // Register contexts with their respective handlers
        webServer?.apply {
            createContext("/file/", FileHttpHandler())
            createContext("/m/batch", BatchHttpHandler())
            executor = null
            start()
        }

        serverUrl = "http://$host:${webServer?.address?.port}"
        logger.info("Web server started at $serverUrl")
    }

    /**
     * Returns the web server URL.
     */
    fun getServerUrl(): String? { return serverUrl }

    /**
     * Returns the base URL for file hosting through the server.
     */
    fun getHostingUrl(): String? { return "$serverUrl/file/" }

    /**
     * Stops the web server.
     *
     * This method is safe for repeated calls and correctly handles cases
     * where the server was already stopped or not initialized.
     */
    override fun close() {
        try {
            webServer?.stop(0)
            logger.info("Web server stopped at $serverUrl")
        } catch (e: Exception) {
            logger.error("Error while stopping web server: ${e.message}", e)
        } finally {
            webServer = null
            serverUrl = null
        }
    }

    /**
     * HTTP handler for serving files (/file/).
     */
    private inner class FileHttpHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            // Yield thread execution if the server is paused
            while (paused) { Thread.yield() }

            val path = exchange.requestURI.path.replaceFirst(exchange.httpContext.path, "")
            logger.info("Sending file to client: $path")
            val file = File(path)

            exchange.sendResponseHeaders(200, file.length())
            exchange.responseBody.use { stream -> Files.copy(file.toPath(), stream) }
        }
    }

    /**
     * HTTP handler for receiving events (/m/batch).
     *
     * Saves each event to the local storage [EventStorage].
     */
    private inner class BatchHttpHandler : HttpHandler {
        private val json = Json { ignoreUnknownKeys = true }

        override fun handle(exchange: HttpExchange) {
            while (paused) Thread.yield()

            val body = exchange.requestBody.bufferedReader().use { it.readText() }
            val root = json.parseToJsonElement(body).jsonObject
            val metaData = root["meta"] ?: JsonNull
            val eventsData = root["events"]?.jsonArray ?: JsonArray(emptyList())

            // Base number for new events
            val baseNum = EventStorage.getLastEvent()?.event_num ?: 0

            // Build list of events
            val parsedEvent = eventsData.mapIndexed { idx, evJson ->
                val evObj = evJson.jsonObject
                val evTime = evObj["event_time"]?.jsonPrimitive?.content ?: Instant.now().toString()
                val evNum  = evObj["event_num"]?.jsonPrimitive?.intOrNull ?: (baseNum + idx + 1)
                val evName = evObj["name"]?.jsonPrimitive?.content ?: "UNKNOWN"

                // Construct a single JSON record for each event
                val singleRecord = buildJsonObject {
                    put("meta", metaData)
                    put("event", evJson)
                }

                Event(
                    event_time = evTime,
                    event_num  = evNum,
                    name       = evName,
                    data       = EventData(
                        uri           = exchange.requestURI.toString(),
                        remoteAddress = exchange.remoteAddress.toString(),
                        headers       = exchange.requestHeaders.toMap(),
                        query         = exchange.requestURI.rawQuery,
                        body          = singleRecord.toString()
                    )
                )
            }

            EventStorage.addEvents(parsedEvent)

            val response = "OK"
            val bytes = response.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
    }
}
