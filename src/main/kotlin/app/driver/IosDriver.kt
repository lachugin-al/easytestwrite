package app.driver

import app.config.AppConfig
import io.appium.java_client.AppiumDriver
import io.appium.java_client.MobileElement
import io.appium.java_client.ios.IOSDriver
import io.appium.java_client.remote.IOSMobileCapabilityType
import io.appium.java_client.remote.MobileCapabilityType
import org.openqa.selenium.Platform
import org.openqa.selenium.SessionNotCreatedException
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.remote.DesiredCapabilities
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.HashMap

/**
 * Обёртка для инициализации Appium-драйвера для платформы iOS.
 *
 * Управляет процессом подключения к Appium-серверу и обработкой ошибок при запуске сессии.
 *
 * @property autoLaunch Флаг, управляющий автоматическим запуском приложения после старта сессии.
 */
class IosDriver(private val autoLaunch: Boolean) {

    private val logger: Logger = LoggerFactory.getLogger(IosDriver::class.java)

    /**
     * Создаёт экземпляр [io.appium.java_client.AppiumDriver] для iOS-платформы.
     *
     * В случае ошибок создания сессии выполняет повторные попытки в количестве, заданном через [retryCount].
     *
     * @param retryCount Количество оставшихся попыток инициализации драйвера.
     * @return Экземпляр [io.appium.java_client.AppiumDriver]<[io.appium.java_client.MobileElement]> для работы с iOS.
     * @throws RuntimeException в случае исчерпания всех попыток или других ошибок при инициализации.
     */
    fun getIOSDriver(retryCount: Int): AppiumDriver<MobileElement> {
        return try {
            logger.info("Инициализация iOS-драйвера (попыток осталось: $retryCount)")
            IOSDriver(AppConfig.getAppiumUrl(), getCapabilities())
        } catch (e: SessionNotCreatedException) {
            logger.error("Ошибка создания сессии iOS-драйвера", e)
            if (retryCount > 0) {
                logger.warn("Повторная попытка инициализации iOS-драйвера (осталось ${retryCount - 1})")
                return getIOSDriver(retryCount - 1)
            } else {
                logger.error(
                    """
                    Не удалось создать сессию iOS-драйвера после нескольких попыток.
                    Проверьте правильность версии платформы и имени устройства.
                    Для получения списка доступных симуляторов выполните: 'xcrun simctl list devices available'
                    """.trimIndent(), e
                )
                throw RuntimeException(
                    """
                    Не удалось инициализировать iOS-драйвер.
                    Проверьте правильность версии платформы и имени устройства.
                    Для просмотра доступных симуляторов выполните: 'xcrun simctl list devices available'
                    """.trimIndent(), e
                )
            }
        } catch (e: WebDriverException) {
            logger.error("Ошибка подключения к Appium-серверу для iOS", e)
            throw RuntimeException("Не удалось подключиться к Appium-серверу для iOS", e)
        } catch (e: IOException) {
            logger.error("Ошибка IO при инициализации iOS-драйвера", e)
            throw RuntimeException("Ошибка IO при инициализации iOS-драйвера", e)
        }
    }

    /**
     * Формирует объект [org.openqa.selenium.remote.DesiredCapabilities] для создания сессии iOS-драйвера.
     *
     * Задаёт параметры запуска, автоматическую обработку алертов, настройки клавиатуры и прочие параметры
     * специфичные для тестирования на платформе iOS.
     *
     * @return Конфигурация возможностей для запуска iOS-приложения через Appium.
     * @throws RuntimeException если файл приложения не найден.
     */
    private fun getCapabilities(): DesiredCapabilities {
        val appFile = File(AppConfig.getAppName())
        if (!appFile.exists()) {
            throw RuntimeException(
                """
                Не найден файл приложения: ${AppConfig.getAppName()}.
                Ожидалось наличие файла по пути: ${appFile.absolutePath}.
                Скомпилируйте iOS-приложение и скопируйте .app файл в корень проекта.
                """.trimIndent()
            )
        }

        logger.info("Формирование DesiredCapabilities для iOS-драйвера")
        val capabilities = DesiredCapabilities()
        capabilities.setCapability(MobileCapabilityType.APP, appFile.absolutePath)
        capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, "XCuiTest")
        capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, Platform.IOS)
        capabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, AppConfig.getIosVersion())
        capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, AppConfig.getIosDeviceName())
        capabilities.setCapability(IOSMobileCapabilityType.CONNECT_HARDWARE_KEYBOARD, false)
        capabilities.setCapability(IOSMobileCapabilityType.AUTO_ACCEPT_ALERTS, true)
        capabilities.setCapability(IOSMobileCapabilityType.SHOW_IOS_LOG, false)
        capabilities.setCapability("appium:autoLaunch", autoLaunch)

        val processArguments = HashMap<String, Array<String>>()
        capabilities.setCapability(IOSMobileCapabilityType.PROCESS_ARGUMENTS, processArguments)
        capabilities.setCapability("settings[customSnapshotTimeout]", 3)

        logger.info("DesiredCapabilities для iOS сформированы успешно")
        return capabilities
    }
}