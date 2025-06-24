package unit.proxy

import events.Event
import events.EventStorage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import proxy.WebServer
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.lang.reflect.Field

/**
 * Модульные тесты для класса [WebServer].
 *
 * Тестирует основную функциональность веб-сервера:
 * - Запуск и остановка сервера
 * - Обслуживание файлов
 * - Обработка пакетных запросов
 * - Обработка ошибок
 * - Управление состоянием паузы
 */
class WebServerTest {

    private lateinit var webServer: WebServer
    private lateinit var tempFile: File

    @BeforeEach
    fun setUp() {
        // Очищаем хранилище событий
        EventStorage.clear()

        // Создаем временный файл для тестирования обслуживания файлов
        tempFile = File.createTempFile("test", ".txt")
        tempFile.writeText("Test file content")

        // Инициализируем веб-сервер
        webServer = WebServer()
        webServer.start()
    }

    @AfterEach
    fun tearDown() {
        // Останавливаем веб-сервер
        webServer.close()

        // Удаляем временный файл
        tempFile.delete()

        // Очищаем хранилище событий
        EventStorage.clear()
    }

    @Test
    fun `test server starts and returns valid URL`() {
        // Проверяем, что URL сервера не null и имеет правильный формат
        val serverUrl = webServer.getServerUrl()
        assertNotNull(serverUrl)
        assertTrue(serverUrl!!.startsWith("http://"))

        // Проверяем, что URL хостинга получен из URL сервера
        val hostingUrl = webServer.getHostingUrl()
        assertNotNull(hostingUrl)
        assertTrue(hostingUrl!!.startsWith(serverUrl))
        assertTrue(hostingUrl.endsWith("/file/"))
    }

    @Test
    fun `test server can be closed and restarted`() {
        // Получаем начальный URL сервера
        val initialUrl = webServer.getServerUrl()

        // Закрываем сервер
        webServer.close()

        // Проверяем, что URL сервера null после закрытия
        assertNull(webServer.getServerUrl())

        // Перезапускаем сервер
        webServer.start()

        // Проверяем, что новый URL сервера не null
        val newUrl = webServer.getServerUrl()
        assertNotNull(newUrl)
    }

    @Test
    fun `test batch endpoint processes events`() {
        // Пропускаем, если URL сервера null
        val serverUrl = webServer.getServerUrl() ?: return

        // Создаем JSON пакетного запроса
        val batchJson = buildJsonObject {
            putJsonObject("meta") {
                put("app_version", "1.0.0")
                put("platform", "test")
            }
            putJsonArray("events") {
                add(buildJsonObject {
                    put("event_time", "2023-01-01T12:00:00Z")
                    put("event_num", 1)
                    put("name", "test_event")
                    putJsonObject("data") {
                        put("key1", "value1")
                        put("key2", "value2")
                    }
                })
            }
        }.toString()

        // Отправляем POST запрос на конечную точку batch
        val url = URL("$serverUrl/m/batch")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")

        connection.outputStream.use { it.write(batchJson.toByteArray()) }

        // Проверяем ответ
        assertEquals(200, connection.responseCode)

        // Проверяем, что событие было сохранено
        val events = EventStorage.getEvents()
        assertEquals(1, events.size)
        assertEquals("test_event", events[0].name)
    }

    @Test
    fun `test file serving endpoint`() {
        // Пропускаем, если URL сервера null
        val serverUrl = webServer.getServerUrl() ?: return

        // Получаем абсолютный путь к временному файлу
        val filePath = tempFile.absolutePath

        // Создаем URL для запроса файла
        val url = URL("$serverUrl/file/$filePath")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        // Проверяем ответ
        assertEquals(200, connection.responseCode)

        // Читаем содержимое ответа
        val content = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }

        // Проверяем, что содержимое соответствует содержимому файла
        assertEquals("Test file content", content)
    }

    @Test
    fun `test server paused state`() {
        // Пропускаем, если URL сервера null
        val serverUrl = webServer.getServerUrl() ?: return

        // Устанавливаем сервер в состояние паузы через рефлексию
        val pausedField = WebServer::class.java.getDeclaredField("paused")
        pausedField.isAccessible = true
        pausedField.set(webServer, true)

        try {
            // Создаем URL для запроса с таймаутом
            val url = URL("$serverUrl/file/${tempFile.absolutePath}")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 500 // Устанавливаем короткий таймаут
            connection.readTimeout = 500
            connection.requestMethod = "GET"

            // Пытаемся получить ответ - должен быть таймаут из-за паузы
            val exception = assertThrows(Exception::class.java) {
                connection.inputStream.use { it.readAllBytes() }
            }

            // Проверяем, что произошла ошибка таймаута
            assertTrue(exception.message?.contains("timed out") ?: false || 
                       exception.message?.contains("timeout") ?: false)
        } finally {
            // Восстанавливаем состояние сервера
            pausedField.set(webServer, false)
        }
    }

    @Test
    fun `test batch endpoint with multiple events`() {
        // Пропускаем, если URL сервера null
        val serverUrl = webServer.getServerUrl() ?: return

        // Создаем JSON пакетного запроса с несколькими событиями
        val batchJson = buildJsonObject {
            putJsonObject("meta") {
                put("app_version", "1.0.0")
                put("platform", "test")
            }
            putJsonArray("events") {
                // Первое событие
                add(buildJsonObject {
                    put("event_time", "2023-01-01T12:00:00Z")
                    put("event_num", 1)
                    put("name", "test_event_1")
                    putJsonObject("data") {
                        put("key1", "value1")
                    }
                })
                // Второе событие
                add(buildJsonObject {
                    put("event_time", "2023-01-01T12:01:00Z")
                    put("event_num", 2)
                    put("name", "test_event_2")
                    putJsonObject("data") {
                        put("key2", "value2")
                    }
                })
            }
        }.toString()

        // Отправляем POST запрос на конечную точку batch
        val url = URL("$serverUrl/m/batch")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")

        connection.outputStream.use { it.write(batchJson.toByteArray()) }

        // Проверяем ответ
        assertEquals(200, connection.responseCode)

        // Проверяем, что оба события были сохранены
        val events = EventStorage.getEvents()
        assertEquals(2, events.size)
        assertTrue(events.any { it.name == "test_event_1" && it.event_num == 1 })
        assertTrue(events.any { it.name == "test_event_2" && it.event_num == 2 })
    }
}
