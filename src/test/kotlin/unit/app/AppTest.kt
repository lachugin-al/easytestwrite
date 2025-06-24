package unit.app

import app.App
import app.config.AppConfig
import app.model.Platform
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import proxy.WebServer
import java.lang.reflect.Field

/**
 * Модульные тесты для класса [App].
 *
 * Тестирует основную функциональность управления приложением:
 * - Создание экземпляра App
 * - Проверка инициализации WebServer
 * - Тестирование жизненного цикла приложения
 *
 * Примечание: Тесты, требующие создания драйвера (launch, close с реальным драйвером),
 * отключены, так как они требуют подключения к Appium серверу.
 */
class AppTest {

    private lateinit var app: App

    // Сохраняем исходную платформу для восстановления после тестов
    private var originalPlatform: Platform? = null

    @BeforeEach
    fun setUp() {
        // Сохраняем исходную платформу
        originalPlatform = AppConfig.getPlatform()

        // Создаем экземпляр приложения
        app = App()
    }

    @AfterEach
    fun tearDown() {
        // Закрываем приложение
        app.close()

        // Восстанавливаем исходную платформу, если она изменилась
        if (originalPlatform != null && originalPlatform != AppConfig.getPlatform()) {
            try {
                val platformField = AppConfig::class.java.getDeclaredField("platform")
                platformField.isAccessible = true
                platformField.set(null, originalPlatform)
            } catch (e: Exception) {
                // Если прямой доступ к полю не удался, логируем ошибку
                println("Не удалось восстановить исходную платформу: ${e.message}")
            }
        }
    }

    @Test
    fun `test app instance creation`() {
        // Проверяем, что экземпляр приложения создан
        assertNotNull(app)

        // Проверяем, что app.current установлен
        assertEquals(app, App.current)

        // Проверяем, что WebServer инициализирован
        val webServerField = App::class.java.getDeclaredField("webServer")
        webServerField.isAccessible = true
        val webServer = webServerField.get(app) as WebServer
        assertNotNull(webServer)
    }

    @Test
    fun `test webserver initialization`() {
        // Получаем ссылку на WebServer
        val webServerField = App::class.java.getDeclaredField("webServer")
        webServerField.isAccessible = true
        val webServer = webServerField.get(app) as WebServer

        // Проверяем, что WebServer создан, но еще не запущен
        assertNotNull(webServer)
        assertNull(webServer.getServerUrl())

        // Запускаем WebServer напрямую
        webServer.start()

        try {
            // Проверяем, что WebServer запущен
            assertNotNull(webServer.getServerUrl())
            assertTrue(webServer.getServerUrl()!!.startsWith("http://"))

            // Проверяем URL хостинга
            val hostingUrl = webServer.getHostingUrl()
            assertNotNull(hostingUrl)
            assertTrue(hostingUrl!!.endsWith("/file/"))
        } finally {
            // Останавливаем WebServer
            webServer.close()
        }
    }

    @Test
    fun `test webserver start and stop`() {
        // Получаем ссылку на WebServer
        val webServerField = App::class.java.getDeclaredField("webServer")
        webServerField.isAccessible = true
        val webServer = webServerField.get(app) as WebServer

        // Запускаем WebServer
        webServer.start()

        // Проверяем, что WebServer запущен
        val serverUrl = webServer.getServerUrl()
        assertNotNull(serverUrl)

        // Останавливаем WebServer
        webServer.close()

        // Проверяем, что WebServer остановлен
        assertNull(webServer.getServerUrl())
    }
}
