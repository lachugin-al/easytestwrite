package app.appium.driver

import app.config.AppConfig
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
 * Encapsulates initialization of the Appium Android driver.
 *
 * Supports configuring the app launch behavior and controlling the number of
 * reconnection attempts in case of session start errors.
 *
 * @property autoLaunch flag indicating whether the app should be auto-launched after the driver starts
 */
class AndroidDriver(private val autoLaunch: Boolean) {

    private val logger: Logger = LoggerFactory.getLogger(AndroidDriver::class.java)

    /**
     * Initializes an instance of [AppiumDriver] for Android.
     *
     * If a session creation error (SessionNotCreatedException) occurs, the method will perform
     * retries according to the [retryCount] value. For other error types, it fails immediately.
     *
     * @param retryCount remaining number of attempts to initialize the driver
     * @return a properly initialized instance of [AppiumDriver]<[MobileElement]>
     * @throws RuntimeException if the session cannot be created after all attempts
     */
    fun getAndroidDriver(retryCount: Int): AppiumDriver<MobileElement> {
        return try {
            logger.info("Initializing Android driver (attempts left: $retryCount)")
            AndroidDriver(AppConfig.getAppiumUrl(), getCapabilities())
        } catch (e: SessionNotCreatedException) {
            logger.error("Failed to create Appium driver session", e)
            if (retryCount > 0) {
                logger.warn("Retrying Android driver initialization (remaining: ${retryCount - 1})")
                return getAndroidDriver(retryCount - 1)
            } else {
                logger.error("Unable to create Android driver session after all attempts", e)
                throw RuntimeException("Failed to initialize Android driver. Ensure the emulator is running.", e)
            }
        } catch (e: WebDriverException) {
            logger.error("Error connecting to the Appium server", e)
            throw RuntimeException("Could not connect to the Appium server", e)
        } catch (e: MalformedURLException) {
            logger.error("Invalid Appium server URL format", e)
            throw RuntimeException("Appium server URL format error", e)
        }
    }

    /**
     * Builds and returns a [DesiredCapabilities] object for Android device configuration.
     *
     * Sets parameters for the APK path, platform version, device name, and session behavior
     * (timeouts, auto-launch, auto-grant permissions, etc.).
     *
     * @return an instance of [DesiredCapabilities] ready to create an Appium session
     * @throws RuntimeException if the app APK file is not found at the expected location
     */
    private fun getCapabilities(): DesiredCapabilities {
        val appFile = File(AppConfig.getAppName())
        if (!appFile.exists()) {
            throw RuntimeException("""
                App APK file '${AppConfig.getAppName()}' was not found.
                Expected file path: ${appFile.absolutePath}.
                Build the Android app and copy the APK to the project root.
            """.trimIndent())
        }

        logger.info("Building DesiredCapabilities for the Android driver")
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

        logger.info("DesiredCapabilities built successfully")
        return capabilities
    }
}
