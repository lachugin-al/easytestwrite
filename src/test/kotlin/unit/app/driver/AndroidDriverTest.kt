package unit.app.driver

import app.driver.AndroidDriver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled

/**
 * Модульные тесты для класса [AndroidDriver].
 *
 * Тестирует основную функциональность драйвера Android:
 * - Создание экземпляра AndroidDriver с разными параметрами
 * - Проверка конструктора
 *
 * Примечание: Тесты не запускают реальный драйвер и не подключаются к Appium-серверу.
 */
class AndroidDriverTest {

    /**
     * Тестирует создание экземпляра AndroidDriver.
     */
    @Test
    fun `test android driver instance creation`() {
        // Создаем экземпляр AndroidDriver с autoLaunch = true
        val androidDriver = AndroidDriver(true)

        // Проверяем, что экземпляр AndroidDriver создан
        assertNotNull(androidDriver)
    }

    /**
     * Тестирует конструктор AndroidDriver с разными значениями autoLaunch.
     */
    @Test
    fun `test constructor with different autoLaunch values`() {
        // Создаем экземпляры AndroidDriver с разными значениями autoLaunch
        val androidDriverWithAutoLaunchTrue = AndroidDriver(true)
        val androidDriverWithAutoLaunchFalse = AndroidDriver(false)

        // Проверяем, что экземпляры созданы
        assertNotNull(androidDriverWithAutoLaunchTrue)
        assertNotNull(androidDriverWithAutoLaunchFalse)
    }
}
