package unit.proxy

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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.net.URI

/**
 * Unit tests for [WebServer].
 *
 * Verifies the core functionality of the web server:
 * - Starting and stopping the server
 * - Serving files
 * - Handling batch requests
 * - Error handling
 * - Managing the paused state
 */
class WebServerTest {

    private lateinit var webServer: WebServer
    private lateinit var tempFile: File

    @BeforeEach
    fun setUp() {
        // Clear the event storage
        EventStorage.clear()

        // Create a temporary file for testing file serving
        tempFile = File.createTempFile("test", ".txt")
        tempFile.writeText("Test file content")

        // Initialize and start the web server
        webServer = WebServer()
        webServer.start()
    }

    @AfterEach
    fun tearDown() {
        // Stop the web server
        webServer.close()

        // Delete the temporary file
        tempFile.delete()

        // Clear the event storage
        EventStorage.clear()
    }

    @Test
    fun `test server starts and returns valid URL`() {
        // Verify the server URL is not null and has the correct format
        val serverUrl = webServer.getServerUrl()
        assertNotNull(serverUrl)
        assertTrue(serverUrl!!.startsWith("http://"))

        // Verify that the hosting URL is derived from the server URL
        val hostingUrl = webServer.getHostingUrl()
        assertNotNull(hostingUrl)
        assertTrue(hostingUrl!!.startsWith(serverUrl))
        assertTrue(hostingUrl.endsWith("/file/"))
    }

    @Test
    fun `test server can be closed and restarted`() {
        // Get the initial server URL
        val initialUrl = webServer.getServerUrl()

        // Close the server
        webServer.close()

        // Verify the server URL is null after closing
        assertNull(webServer.getServerUrl())

        // Restart the server
        webServer.start()

        // Verify the new server URL is not null
        val newUrl = webServer.getServerUrl()
        assertNotNull(newUrl)
    }

    @Test
    fun `test batch endpoint processes events`() {
        // Skip if server URL is null
        val serverUrl = webServer.getServerUrl() ?: return

        // Create a JSON payload for the batch request
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

        // Send a POST request to the batch endpoint
        val url = URI("$serverUrl/m/batch").toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")

        connection.outputStream.use { it.write(batchJson.toByteArray()) }

        // Verify the response
        assertEquals(200, connection.responseCode)

        // Verify the event was stored
        val events = EventStorage.getEvents()
        assertEquals(1, events.size)
        assertEquals("test_event", events[0].name)
    }

    @Test
    fun `test file serving endpoint`() {
        // Skip if server URL is null
        val serverUrl = webServer.getServerUrl() ?: return

        // Get the absolute path to the temporary file
        val filePath = tempFile.absolutePath

        // Build the URL to request the file
        val url = URI("$serverUrl/file/$filePath").toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        // Verify the response
        assertEquals(200, connection.responseCode)

        // Read the response content
        val content = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }

        // Verify the content matches the file's content
        assertEquals("Test file content", content)
    }

    @Test
    fun `test server paused state`() {
        // Skip if server URL is null
        val serverUrl = webServer.getServerUrl() ?: return

        // Set the server to a paused state via reflection
        val pausedField = WebServer::class.java.getDeclaredField("paused")
        pausedField.isAccessible = true
        pausedField.set(webServer, true)

        try {
            // Create a URL for a request with a short timeout
            val url = URI("$serverUrl/file/${tempFile.absolutePath}").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 500 // Short timeout
            connection.readTimeout = 500
            connection.requestMethod = "GET"

            // Attempt to read the response — should time out due to pause
            val exception = assertThrows(Exception::class.java) {
                connection.inputStream.use { it.readAllBytes() }
            }

            // Verify a timeout-related error occurred
            assertTrue(
                exception.message?.contains("timed out") ?: false ||
                        exception.message?.contains("timeout") ?: false
            )
        } finally {
            // Restore server state
            pausedField.set(webServer, false)
        }
    }

    @Test
    fun `test batch endpoint with multiple events`() {
        // Skip if server URL is null
        val serverUrl = webServer.getServerUrl() ?: return

        // Create a JSON payload with multiple events
        val batchJson = buildJsonObject {
            putJsonObject("meta") {
                put("app_version", "1.0.0")
                put("platform", "test")
            }
            putJsonArray("events") {
                // First event
                add(buildJsonObject {
                    put("event_time", "2023-01-01T12:00:00Z")
                    put("event_num", 1)
                    put("name", "test_event_1")
                    putJsonObject("data") {
                        put("key1", "value1")
                    }
                })
                // Second event
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

        // Send a POST request to the batch endpoint
        val url = URI("$serverUrl/m/batch").toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")

        connection.outputStream.use { it.write(batchJson.toByteArray()) }

        // Verify the response
        assertEquals(200, connection.responseCode)

        // Verify both events were stored
        val events = EventStorage.getEvents()
        assertEquals(2, events.size)
        assertTrue(events.any { it.name == "test_event_1" && it.event_num == 1 })
        assertTrue(events.any { it.name == "test_event_2" && it.event_num == 2 })
    }
}
