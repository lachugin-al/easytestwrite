package app

import io.appium.java_client.AppiumDriver
import io.appium.java_client.MobileElement
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.remote.AndroidMobileCapabilityType
import io.appium.java_client.remote.MobileCapabilityType
import org.openqa.selenium.Platform
import org.openqa.selenium.SessionNotCreatedException
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.remote.DesiredCapabilities
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.net.MalformedURLException

/**
 * Инкапсулирует инициализацию Appium Android-драйвера.
 *
 * Поддерживает настройку launch-поведения приложения и управление количеством попыток
 * переподключения в случае ошибок при старте сессии.
 *
 * @property autoLaunch флаг, указывающий на необходимость автозапуска приложения после старта драйвера
 */
class AndroidDriver(private val autoLaunch: Boolean) {

    private val logger: Logger = LoggerFactory.getLogger(AndroidDriver::class.java)

    /**
     * Инициализирует экземпляр [AppiumDriver] для работы с Android-платформой.
     *
     * При возникновении ошибки создания сессии (SessionNotCreatedException) метод выполнит повторные попытки
     * согласно значению [retryCount]. В случае других типов ошибок производится немедленная обработка.
     *
     * @param retryCount количество оставшихся попыток инициализации драйвера
     * @return корректно инициализированный экземпляр [AppiumDriver]<[MobileElement]>
     * @throws RuntimeException если создать сессию не удалось после всех попыток
     */
    fun getAndroidDriver(retryCount: Int): AppiumDriver<MobileElement> {
        return try {
            logger.info("Инициализация Android-драйвера (осталось попыток: $retryCount")
            AndroidDriver(AppConfig.getAppiumUrl(), getCapabilities())
        } catch (e: SessionNotCreatedException) {
            logger.error("Ошибка создания сессии Appium-драйвера", e)
            if (retryCount > 0) {
                logger.warn("Повторная попытка инициализации Android-драйвера (осталось ${retryCount - 1})")
                return getAndroidDriver(retryCount - 1)
            } else {
                logger.error("Не удалось создать сессию Android-драйвера после всех попыток", e)
                throw RuntimeException("Не удалось инициализировать Android-драйвер. Проверьте запущен ли эмулятор.", e)
            }
        } catch (e: WebDriverException) {
            logger.error("Ошибка подключения к Appium-серверу", e)
            throw RuntimeException("Не удалось подключиться к Appium-серверу", e)
        } catch (e: MalformedURLException) {
            logger.error("Неверный формат URL Appium-сервера", e)
            throw RuntimeException("Ошибка формата URL Appium-сервера", e)
        }
    }

    /**
     * Формирует и возвращает объект [DesiredCapabilities] для конфигурации подключения к Android-устройству.
     *
     * В рамках настройки устанавливаются параметры пути к APK-файлу, версии платформы, имени устройства,
     * настройки поведения сессии (таймауты, автозапуск, автоматическая выдача разрешений и прочее).
     *
     * @return экземпляр [DesiredCapabilities], готовый к использованию для создания сессии Appium
     * @throws RuntimeException если APK-файл приложения не найден в ожидаемом месте
     */
    private fun getCapabilities(): DesiredCapabilities {
        val appFile = File(AppConfig.getAppName())
        if (!appFile.exists()) {
            throw RuntimeException("""
                APK-файл приложения '${AppConfig.getAppName()}' не найден.
                Ожидалось наличие файла по пути: ${appFile.absolutePath}.
                Скомпилируйте Android-приложение и скопируйте APK в корень проекта.
            """.trimIndent())
        }

        logger.info("Формирование DesiredCapabilities для Android-драйвера")
        val capabilities = DesiredCapabilities()
        capabilities.setCapability(MobileCapabilityType.APP, appFile.absolutePath)
        capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, "UIAutomator2")
        capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, Platform.ANDROID)
        capabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, AppConfig.getAndroidVersion())
        capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, AppConfig.getAndroidDeviceName())
        capabilities.setCapability(MobileCapabilityType.NO_RESET, false)
        capabilities.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 100)
        capabilities.setCapability(AndroidMobileCapabilityType.DONT_STOP_APP_ON_RESET, false)
        capabilities.setCapability(AndroidMobileCapabilityType.UNICODE_KEYBOARD, true)
        capabilities.setCapability(AndroidMobileCapabilityType.ADB_EXEC_TIMEOUT, 40_000)
        capabilities.setCapability(AndroidMobileCapabilityType.AUTO_GRANT_PERMISSIONS, true)
        capabilities.setCapability("autoLaunch", autoLaunch)
        capabilities.setCapability(AndroidMobileCapabilityType.APP_ACTIVITY, AppConfig.getAppActivity())
        capabilities.setCapability(AndroidMobileCapabilityType.APP_PACKAGE, AppConfig.getAppPackage())

        logger.info("DesiredCapabilities успешно сформированы")
        return capabilities
    }
}