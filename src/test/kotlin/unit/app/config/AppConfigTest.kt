package unit.app.config

import app.config.AppConfig
import app.model.Platform
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.reflect.Field
import java.net.URL

/**
 * Модульные тесты для класса [AppConfig].
 *
 * Тестирует основную функциональность конфигурационного класса:
 * - Загрузка конфигурации
 * - Получение параметров для разных платформ
 * - Проверка значений по умолчанию
 */
class AppConfigTest {

    /**
     * Тестирует получение текущей платформы.
     */
    @Test
    fun `test get platform`() {
        // Получаем текущую платформу
        val platform = AppConfig.getPlatform()

        // Проверяем, что платформа не null и является одним из допустимых значений
        assertNotNull(platform)
        assertTrue(platform in Platform.values())
    }

    /**
     * Тестирует методы проверки текущей платформы.
     */
    @Test
    fun `test platform check methods`() {
        // Получаем текущую платформу
        val currentPlatform = AppConfig.getPlatform()

        // Проверяем согласованность методов определения платформы
        when (currentPlatform) {
            Platform.ANDROID -> {
                assertTrue(AppConfig.isAndroid())
                assertFalse(AppConfig.isiOS())
            }

            Platform.IOS -> {
                assertFalse(AppConfig.isAndroid())
                assertTrue(AppConfig.isiOS())
            }
        }

        // Проверяем, что getPlatform возвращает ожидаемую платформу
        assertEquals(currentPlatform, AppConfig.getPlatform())
    }

    /**
     * Тестирует получение URL Appium-сервера.
     */
    @Test
    fun `test get appium url`() {
        // Получаем URL Appium-сервера
        val appiumUrl = AppConfig.getAppiumUrl()

        // Проверяем, что URL не null и имеет правильный формат
        assertNotNull(appiumUrl)
        assertTrue(appiumUrl.toString().startsWith("http://"))
    }

    /**
     * Тестирует получение имени приложения для текущей платформы.
     */
    @Test
    fun `test get app name for current platform`() {
        // Получаем текущую платформу
        val currentPlatform = AppConfig.getPlatform()

        // Получаем имя приложения
        val appName = AppConfig.getAppName()

        // Проверяем имя приложения в зависимости от текущей платформы
        when (currentPlatform) {
            Platform.ANDROID, Platform.IOS -> {
                assertNotNull(appName)
                assertFalse(appName.isEmpty())
            }
        }
    }

    /**
     * Тестирует получение настроек видеозаписи в зависимости от платформы.
     */
    @Test
    fun `test video recording settings`() {
        // Проверяем настройки видеозаписи
        val isEnabled = AppConfig.isVideoRecordingEnabled()
        val size = AppConfig.getVideoRecordingSize()
        val quality = AppConfig.getVideoRecordingQuality()
        val bitrate = AppConfig.getVideoRecordingBitrate()
        val outputDir = AppConfig.getVideoRecordingOutputDir()

        // Проверяем, что все значения не null и имеют ожидаемый формат
        assertNotNull(size)
        assertTrue(size.matches(Regex("\\d+x\\d+")))
        assertTrue(quality in 0..100)
        assertTrue(bitrate > 0)
        assertNotNull(outputDir)
        assertFalse(outputDir.isEmpty())
    }

    /**
     * Вспомогательный метод для установки значения приватного поля.
     */
    private fun setPrivateField(clazz: Class<*>, fieldName: String, value: Any) {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(null, value)
    }
}
