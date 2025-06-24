package unit.app.driver

import app.driver.IosDriver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled

/**
 * Модульные тесты для класса [IosDriver].
 *
 * Тестирует основную функциональность драйвера iOS:
 * - Создание экземпляра IosDriver с разными параметрами
 * - Проверка конструктора
 *
 * Примечание: Тесты не запускают реальный драйвер и не подключаются к Appium-серверу.
 */
class IosDriverTest {

    /**
     * Тестирует создание экземпляра IosDriver.
     */
    @Test
    fun `test ios driver instance creation`() {
        // Создаем экземпляр IosDriver с autoLaunch = true
        val iosDriver = IosDriver(true)

        // Проверяем, что экземпляр IosDriver создан
        assertNotNull(iosDriver)
    }

    /**
     * Тестирует конструктор IosDriver с разными значениями autoLaunch.
     */
    @Test
    fun `test constructor with different autoLaunch values`() {
        // Создаем экземпляры IosDriver с разными значениями autoLaunch
        val iosDriverWithAutoLaunchTrue = IosDriver(true)
        val iosDriverWithAutoLaunchFalse = IosDriver(false)

        // Проверяем, что экземпляры созданы
        assertNotNull(iosDriverWithAutoLaunchTrue)
        assertNotNull(iosDriverWithAutoLaunchFalse)
    }
}
