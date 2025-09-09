package unit.app

import app.App
import app.config.AppConfig
import app.model.Platform
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import proxy.WebServer

/**
 * Unit tests for [App].
 *
 * Tests core functionality of application management:
 * - Creating an instance of App
 * - Verifying WebServer initialization
 * - Testing the application lifecycle
 *
 * Note: Tests that require creating a real driver (launch, close with a real driver)
 * are disabled because they require a connection to an Appium server.
 */
class AppTest {

    private lateinit var app: App

    // Save the original platform to restore after tests
    private var originalPlatform: Platform? = null

    @BeforeEach
    fun setUp() {
        // Save the original platform
        originalPlatform = AppConfig.getPlatform()

        // Create the application instance
        app = App()
    }

    @AfterEach
    fun tearDown() {
        // Close the application
        app.close()

        // Restore the original platform if it changed
        if (originalPlatform != null && originalPlatform != AppConfig.getPlatform()) {
            try {
                val platformField = AppConfig::class.java.getDeclaredField("platform")
                platformField.isAccessible = true
                platformField.set(null, originalPlatform)
            } catch (e: Exception) {
                // If direct field access failed, log the error
                println("Failed to restore original platform: ${e.message}")
            }
        }
    }

    @Test
    fun `test app instance creation`() {
        // Verify the application instance is created
        assertNotNull(app)

        // Verify that app.current is set
        assertEquals(app, App.current)

        // Verify that WebServer is initialized
        val webServerField = App::class.java.getDeclaredField("webServer")
        webServerField.isAccessible = true
        val webServer = webServerField.get(app) as WebServer
        assertNotNull(webServer)
    }

    @Test
    fun `test webserver initialization`() {
        // Get a reference to WebServer
        val webServerField = App::class.java.getDeclaredField("webServer")
        webServerField.isAccessible = true
        val webServer = webServerField.get(app) as WebServer

        // Verify WebServer is created but not started yet
        assertNotNull(webServer)
        assertNull(webServer.getServerUrl())

        // Start WebServer directly
        webServer.start()

        try {
            // Verify WebServer is running
            assertNotNull(webServer.getServerUrl())
            assertTrue(webServer.getServerUrl()!!.startsWith("http://"))

            // Verify hosting URL
            val hostingUrl = webServer.getHostingUrl()
            assertNotNull(hostingUrl)
            assertTrue(hostingUrl!!.endsWith("/file/"))
        } finally {
            // Stop WebServer
            webServer.close()
        }
    }

    @Test
    fun `test webserver start and stop`() {
        // Get a reference to WebServer
        val webServerField = App::class.java.getDeclaredField("webServer")
        webServerField.isAccessible = true
        val webServer = webServerField.get(app) as WebServer

        // Start WebServer
        webServer.start()

        // Verify WebServer is running
        val serverUrl = webServer.getServerUrl()
        assertNotNull(serverUrl)

        // Stop WebServer
        webServer.close()

        // Verify WebServer is stopped
        assertNull(webServer.getServerUrl())
    }
}
