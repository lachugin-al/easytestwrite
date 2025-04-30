package app.driver

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Обёртка для инициализации и управления экземпляром браузера Playwright.
 *
 * Поддерживает создание браузеров разных типов (Chromium, Firefox, WebKit) в headless или headful режиме.
 *
 * @property browserType Тип браузера, который необходимо запустить (chromium, firefox, webkit).
 * @property headless Флаг, указывающий на необходимость запуска браузера в headless-режиме.
 */
class WebDriver(private val browserType: String, private val headless: Boolean) {

    private val logger: Logger = LoggerFactory.getLogger(WebDriver::class.java)
    private var playwright: Playwright? = null
    private var browser: Browser? = null

    /**
     * Инициализирует Playwright и возвращает новую страницу [com.microsoft.playwright.Page] для работы с браузером.
     *
     * При возникновении ошибок в процессе запуска выполняет повторные попытки в количестве [retryCount].
     *
     * @param retryCount Количество оставшихся попыток инициализации браузера.
     * @return Инициализированная страница [com.microsoft.playwright.Page] для работы в тестах.
     * @throws RuntimeException в случае исчерпания всех попыток запуска Playwright.
     */
    fun getPlaywrightPage(retryCount: Int): Page {
        return try {
            logger.info("Инициализация Playwright-драйвера (попыток осталось: $retryCount)")
            playwright = Playwright.create()
            val browser = getBrowser(browserType, headless)
            browser.newPage()
        } catch (e: Exception) {
            logger.error("Ошибка при инициализации Playwright", e)
            if (retryCount > 0) {
                logger.warn("Повторная попытка запуска Playwright (осталось ${retryCount - 1})")
                getPlaywrightPage(retryCount - 1)
            } else {
                logger.error("Не удалось запустить Playwright после всех попыток", e)
                throw RuntimeException("Не удалось запустить Playwright после всех попыток", e)
            }
        }
    }

    /**
     * Выбирает и запускает браузер в зависимости от заданного [browserType].
     *
     * Поддерживаемые типы браузеров:
     * - Chromium
     * - Firefox
     * - WebKit
     *
     * @param browserType Тип браузера.
     * @param headless Флаг headless-режима.
     * @return Экземпляр запущенного [Browser].
     * @throws IllegalArgumentException если указан неподдерживаемый тип браузера.
     */
    private fun getBrowser(browserType: String, headless: Boolean): Browser {
        return when (browserType.lowercase()) {
            "chromium" -> playwright!!.chromium().launch(BrowserType.LaunchOptions().setHeadless(headless))
            "firefox" -> playwright!!.firefox().launch(BrowserType.LaunchOptions().setHeadless(headless))
            "webkit" -> playwright!!.webkit().launch(BrowserType.LaunchOptions().setHeadless(headless))
            else -> throw IllegalArgumentException("Указан не поддерживаемый тип браузера: $browserType")
        }
    }

    /**
     * Корректно закрывает запущенный браузер и освобождает ресурсы Playwright.
     *
     * Все ошибки при закрытии подавляются через логирование для предотвращения аварийного завершения тестов.
     */
    fun close() {
        try {
            browser?.close()
            playwright?.close()
            logger.info("Playwright-драйвер закрыт успешно")
        } catch (e: Exception) {
            logger.error("Ошибка при закрытии Playwright-драйвера", e)
        }
    }
}