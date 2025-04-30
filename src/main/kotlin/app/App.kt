package app

import app.config.AppConfig
import app.driver.AndroidDriver
import app.driver.IosDriver
import app.driver.WebDriver
import app.model.Platform
import com.microsoft.playwright.Page
import io.appium.java_client.AppiumDriver
import io.appium.java_client.MobileElement
import org.openqa.selenium.WebDriverException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import proxy.WebServer

/**
 * Основной управляющий класс для инициализации инфраструктуры тестового окружения.
 *
 * В рамках одного экземпляра [App] управляется жизненный цикл:
 * - Драйвера Appium для Android/iOS
 * - Web-драйвера Playwright для веб-тестирования
 * - Локального веб-сервера [WebServer] для обслуживания вспомогательных запросов
 *
 * После выполнения тестов все связанные ресурсы корректно освобождаются.
 */
class App() : AutoCloseable {
    companion object {
        /** Текущий запущенный экземпляр приложения. */
        lateinit var current: App
    }

    init {
        // при создании сохраняем ссылку
        current = this
    }

    private val logger: Logger = LoggerFactory.getLogger(App::class.java)

    /** Экземпляр Appium-драйвера для мобильного тестирования (Android/iOS). */
    var driver: AppiumDriver<MobileElement>? = null
        private set

    /** Экземпляр Playwright Page для веб-тестирования. */
    var webDriver: Page? = null
        private set

    /** Локальный веб-сервер, используемый для вспомогательных целей в тестах. */
    val webServer = WebServer()

    /**
     * Выполняет полную инициализацию окружения:
     * - Создаёт новый драйвер для выбранной платформы
     * - Запускает локальный [WebServer]
     *
     * В случае, если предыдущий драйвер уже существует — он предварительно корректно закрывается.
     *
     * @return текущий экземпляр [App] для удобства чейнинга вызовов
     */
    fun launch(): App {
        driver?.let {
            close()
        }

        createDriver()
        webServer.start()

        return this
    }

    /**
     * Создаёт новый экземпляр драйвера в зависимости от платформы, указанной в [app.config.AppConfig].
     *
     * Поддерживаемые платформы:
     * - Android (AppiumDriver)
     * - iOS (AppiumDriver)
     * - Web (Playwright Page)
     */
    private fun createDriver() {
        when (AppConfig.getPlatform()) {
            Platform.ANDROID -> {
                logger.info("Инициализация Android-драйвера")
                driver = AndroidDriver(autoLaunch = true).getAndroidDriver(3)
            }

            Platform.IOS -> {
                logger.info("Инициализация iOS-драйвера")
                driver = IosDriver(autoLaunch = true).getIOSDriver(3)
            }

            Platform.WEB -> {
                logger.info("Инициализация Playwright для веб-тестирования")
                webDriver = WebDriver(AppConfig.getBrowserType(), AppConfig.isHeadless()).getPlaywrightPage(3)
            }
        }
    }

    /**
     * Корректно завершает работу всех активных компонентов:
     * - Завершает мобильное приложение (если драйвер был инициализирован)
     * - Останавливает локальный веб-сервер
     * - Закрывает Playwright-страницу для веб-тестов
     *
     * Все исключения во время закрытия ресурсов логируются и подавляются для предотвращения остановки процесса.
     */
    override fun close() {
        when (AppConfig.getPlatform()) {
            Platform.ANDROID, Platform.IOS -> {
                driver?.let {
                    try {
                        it.terminateApp("com.wildberries.ru.dev")
                        it.quit()
                    } catch (e: WebDriverException) {
                        logger.error("Ошибка при завершении сессии Appium-драйвера", e)
                    } finally {
                        driver = null
                    }
                }
                webServer.close()
            }

            Platform.WEB -> {
                webDriver?.let {
                    try {
                        it.close()
                    } catch (e: Exception) {
                        logger.error("Ошибка при закрытии Playwright-страницы", e)
                    } finally {
                        webDriver = null
                    }
                }
            }
        }
    }
}