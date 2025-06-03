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
 * Локальный HTTP-сервер для тестовой инфраструктуры.
 *
 * Выполняет функции:
 * - Хостинга локальных файлов (/file/)
 * - Приёма событий от клиента (/m/batch) и их сохранения в [EventStorage]
 *
 * Является частью системы тестирования мобильных и web-приложений.
 */
class WebServer : AutoCloseable {
    private val logger: Logger = LoggerFactory.getLogger(WebServer::class.java)

    private var webServer: HttpServer? = null
    private var serverUrl: String? = null
    private var paused: Boolean = false
    private lateinit var proxyServer: ProxyServer

    init {
        // Регистрируем shutdown hook для корректного завершения
        Runtime.getRuntime().addShutdownHook(Thread { close() })
    }

    /**
     * Запускает веб-сервер и прокси-сервер.
     *
     * Веб-сервер стартует на локальном IP-адресе и фиксированном порту 8000.
     * Прокси-сервер стартует на настроенном IP-адресе и порту.
     */
    fun start() {
        val host = NetworkUtils.getLocalAddress()
        val port = 8000 // NetworkUtils.getFreePort()
        try {
            webServer = HttpServer.create(InetSocketAddress(host, port), 0)
        } catch (e: IOException) {
            throw RuntimeException("Не удалось инициализировать веб-сервер", e)
        }

        // Назначение контекста соответствующим обработчикам
        webServer?.apply {
            createContext("/file/", FileHttpHandler())
            createContext("/m/batch", BatchHttpHandler())
            executor = null
            start()
        }

        serverUrl = "http://$host:${webServer?.address?.port}"
        logger.info("Веб-сервер запущен на $serverUrl")

        // Инициализация и запуск прокси-сервера на том же хосте
        try {
            proxyServer = ProxyServer(host ?: "127.0.0.1", 9090)
            proxyServer.start()
            logger.info("Прокси-сервер запущен на ${host ?: "127.0.0.1"}:9090")
        } catch (e: Exception) {
            logger.error("Не удалось запустить прокси-сервер", e)
            // Продолжаем работу даже если прокси-сервер не запустился
        }
    }

    /**
     * Возвращает URL веб-сервера.
     */
    fun getServerUrl(): String? { return serverUrl }

    /**
     * Возвращает URL для загрузки файлов через сервер.
     */
    fun getHostingUrl(): String? { return "$serverUrl/file/" }

    /**
     * Останавливает веб-сервер и прокси-сервер.
     */
    override fun close() {
        webServer?.stop(0)
        logger.info("Веб-сервер остановлен на $serverUrl")

        try {
            proxyServer.close()
        } catch (e: Exception) {
            logger.error("Ошибка при остановке прокси-сервера", e)
        }
    }

    /**
     * Обработчик HTTP-запросов для получения файлов (/file/).
     */
    private inner class FileHttpHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            // Уступить выполнение потока, если сервер приостановлен
            while (paused) { Thread.yield() }

            val path = exchange.requestURI.path.replaceFirst(exchange.httpContext.path, "")
            logger.info("Отправка файла клиенту: $path")
            val file = File(path)

            exchange.sendResponseHeaders(200, file.length())
            exchange.responseBody.use { stream -> Files.copy(file.toPath(), stream) }
        }
    }

    /**
     * Обработчик HTTP-запросов для приёма событий (/m/batch).
     *
     * Сохраняет каждое событие в локальное хранилище [EventStorage].
     */
    private inner class BatchHttpHandler : HttpHandler {
        private val json = Json { ignoreUnknownKeys = true }

        override fun handle(exchange: HttpExchange) {
            while (paused) Thread.yield()

            val body = exchange.requestBody.bufferedReader().use { it.readText() }
            val root = json.parseToJsonElement(body).jsonObject
            val metaData = root["meta"] ?: JsonNull
            val eventsData = root["events"]?.jsonArray ?: JsonArray(emptyList())

            // Базовый номер для новых событий
            val baseNum = EventStorage.getLastEvent()?.event_num ?: 0

            // Собираем список событий
            val parsedEvent = eventsData.mapIndexed { idx, evJson ->
                val evObj = evJson.jsonObject
                val evTime = evObj["event_time"]?.jsonPrimitive?.content ?: Instant.now().toString()
                val evNum  = evObj["event_num"]?.jsonPrimitive?.intOrNull ?: (baseNum + idx + 1)
                val evName = evObj["name"]?.jsonPrimitive?.content ?: "UNKNOWN"

                // Собираем единый JSON для каждого отдельного события
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
