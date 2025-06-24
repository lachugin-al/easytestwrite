package unit.app.model

import app.model.Platform
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Модульные тесты для перечисления [Platform].
 *
 * Тестирует основную функциональность перечисления Platform:
 * - Проверка наличия всех ожидаемых значений
 * - Проверка количества значений
 * - Проверка корректности имен значений
 */
class PlatformTest {

    /**
     * Тестирует наличие всех ожидаемых значений в перечислении.
     */
    @Test
    fun `test platform enum contains all expected values`() {
        // Проверяем, что перечисление содержит все ожидаемые значения
        val platforms = Platform.values()

        assertTrue(platforms.contains(Platform.ANDROID))
        assertTrue(platforms.contains(Platform.IOS))
        assertTrue(platforms.contains(Platform.WEB))
    }

    /**
     * Тестирует количество значений в перечислении.
     */
    @Test
    fun `test platform enum has expected number of values`() {
        // Проверяем, что перечисление содержит ожидаемое количество значений
        val platforms = Platform.values()

        assertEquals(3, platforms.size)
    }

    /**
     * Тестирует корректность имен значений перечисления.
     */
    @Test
    fun `test platform enum values have correct names`() {
        // Проверяем, что имена значений перечисления соответствуют ожидаемым
        assertEquals("ANDROID", Platform.ANDROID.name)
        assertEquals("IOS", Platform.IOS.name)
        assertEquals("WEB", Platform.WEB.name)
    }

    /**
     * Тестирует корректность порядка значений перечисления.
     */
    @Test
    fun `test platform enum values have correct ordinals`() {
        // Проверяем, что порядковые номера значений перечисления соответствуют ожидаемым
        assertEquals(0, Platform.ANDROID.ordinal)
        assertEquals(1, Platform.IOS.ordinal)
        assertEquals(2, Platform.WEB.ordinal)
    }

    /**
     * Тестирует метод valueOf для перечисления.
     */
    @Test
    fun `test platform enum valueOf method`() {
        // Проверяем, что метод valueOf корректно возвращает значения перечисления
        assertEquals(Platform.ANDROID, Platform.valueOf("ANDROID"))
        assertEquals(Platform.IOS, Platform.valueOf("IOS"))
        assertEquals(Platform.WEB, Platform.valueOf("WEB"))
    }

    /**
     * Тестирует исключение при вызове valueOf с некорректным значением.
     */
    @Test
    fun `test platform enum valueOf throws exception for invalid value`() {
        // Проверяем, что метод valueOf выбрасывает исключение при передаче некорректного значения
        assertThrows(IllegalArgumentException::class.java) {
            Platform.valueOf("INVALID_PLATFORM")
        }
    }
}
