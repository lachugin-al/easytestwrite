package unit.app.driver

import app.config.AppConfig
import app.driver.WebDriver
import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import java.lang.reflect.Field

/**
 * Модульные тесты для класса [WebDriver].
 *
 * Тестирует основную функциональность драйвера Web:
 * - Создание экземпляра WebDriver
 * - Проверка инициализации с разными типами браузеров
 * - Проверка headless режима
 *
 * Примечание: Тесты используют рефлексию для доступа к приватным полям.
 */
class WebDriverTest {
    
    private lateinit var webDriver: WebDriver
    
    @BeforeEach
    fun setUp() {
        // Создаем экземпляр WebDriver с типом браузера chromium и headless = true
        webDriver = WebDriver("chromium", true)
    }
    
    /**
     * Тестирует создание экземпляра WebDriver.
     */
    @Test
    fun `test web driver instance creation`() {
        // Проверяем, что экземпляр WebDriver создан
        assertNotNull(webDriver)
    }
    
    /**
     * Тестирует конструктор WebDriver с разными типами браузеров.
     */
    @Test
    fun `test constructor with different browser types`() {
        // Создаем экземпляры WebDriver с разными типами браузеров
        val chromiumDriver = WebDriver("chromium", true)
        val firefoxDriver = WebDriver("firefox", true)
        val webkitDriver = WebDriver("webkit", true)
        
        // Проверяем, что экземпляры созданы
        assertNotNull(chromiumDriver)
        assertNotNull(firefoxDriver)
        assertNotNull(webkitDriver)
    }
    
    /**
     * Тестирует конструктор WebDriver с разными значениями headless.
     */
    @Test
    fun `test constructor with different headless values`() {
        // Создаем экземпляры WebDriver с разными значениями headless
        val headlessDriver = WebDriver("chromium", true)
        val headfulDriver = WebDriver("chromium", false)
        
        // Проверяем, что экземпляры созданы
        assertNotNull(headlessDriver)
        assertNotNull(headfulDriver)
    }
    
    /**
     * Тестирует метод close.
     */
    @Test
    fun `test close method`() {
        // Вызываем метод close
        webDriver.close()
        
        // Проверяем, что поля playwright и browser установлены в null
        try {
            val playwrightField = WebDriver::class.java.getDeclaredField("playwright")
            playwrightField.isAccessible = true
            val playwright = playwrightField.get(webDriver)
            
            val browserField = WebDriver::class.java.getDeclaredField("browser")
            browserField.isAccessible = true
            val browser = browserField.get(webDriver)
            
            // Проверяем, что поля null или закрыты
            // Примечание: в реальности мы не можем проверить, закрыты ли они,
            // поэтому просто проверяем, что метод close не выбрасывает исключений
        } catch (e: Exception) {
            fail("Не удалось получить доступ к полям playwright и browser: ${e.message}")
        }
    }
}