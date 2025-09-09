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
 * Wrapper for initializing the Appium driver for the iOS platform.
 *
 * Manages the process of connecting to the Appium server and handling errors when starting the session.
 *
 * @property autoLaunch Flag that controls automatic app launch after the session starts.
 */
class IosDriver(private val autoLaunch: Boolean) {

    private val logger: Logger = LoggerFactory.getLogger(IosDriver::class.java)

    /**
     * Creates an instance of [io.appium.java_client.AppiumDriver] for the iOS platform.
     *
     * In case of session creation errors, retries the initialization for the number of attempts
     * defined by [retryCount].
     *
     * @param retryCount Number of remaining initialization attempts.
     * @return Instance of [io.appium.java_client.AppiumDriver]<[io.appium.java_client.MobileElement]> for iOS.
     * @throws RuntimeException if all attempts are exhausted or other initialization errors occur.
     */
    fun getIOSDriver(retryCount: Int): AppiumDriver<MobileElement> {
        return try {
            logger.info("Initializing iOS driver (attempts left: $retryCount)")
            IOSDriver(AppConfig.getAppiumUrl(), getCapabilities())
        } catch (e: SessionNotCreatedException) {
            logger.error("iOS driver session creation error", e)
            if (retryCount > 0) {
                logger.warn("Retrying iOS driver initialization (remaining ${retryCount - 1})")
                return getIOSDriver(retryCount - 1)
            } else {
                logger.error(
                    """
                    Failed to create iOS driver session after multiple attempts.
                    Check the correctness of the platform version and device name.
                    To get a list of available simulators run: 'xcrun simctl list devices available'
                    """.trimIndent(), e
                )
                throw RuntimeException(
                    """
                    Failed to initialize the iOS driver.
                    Check the correctness of the platform version and device name.
                    To view available simulators run: 'xcrun simctl list devices available'
                    """.trimIndent(), e
                )
            }
        } catch (e: WebDriverException) {
            logger.error("Error connecting to Appium server for iOS", e)
            throw RuntimeException("Failed to connect to Appium server for iOS", e)
        } catch (e: IOException) {
            logger.error("IO error during iOS driver initialization", e)
            throw RuntimeException("IO error during iOS driver initialization", e)
        }
    }

    /**
     * Builds the [org.openqa.selenium.remote.DesiredCapabilities] object for creating the iOS driver session.
     *
     * Sets launch parameters, automatic alert handling, keyboard settings, and other
     * options specific to iOS testing.
     *
     * @return Capabilities configuration for launching the iOS app via Appium.
     * @throws RuntimeException if the app file is not found.
     */
    private fun getCapabilities(): DesiredCapabilities {
        val appFile = File(AppConfig.getAppName())
        if (!appFile.exists()) {
            throw RuntimeException(
                """
                Application file not found: ${AppConfig.getAppName()}.
                Expected file path: ${appFile.absolutePath}.
                Build the iOS application and copy the .app file into the project root.
                """.trimIndent()
            )
        }

        logger.info("Building DesiredCapabilities for iOS driver")
        val capabilities = DesiredCapabilities()
        capabilities.setCapability(MobileCapabilityType.APP, appFile.absolutePath)
        capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, "XCUITest")
        capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, Platform.IOS)
        capabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, AppConfig.getIosVersion())
        capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, AppConfig.getIosDeviceName())
        capabilities.setCapability(IOSMobileCapabilityType.CONNECT_HARDWARE_KEYBOARD, false)
        capabilities.setCapability(IOSMobileCapabilityType.AUTO_ACCEPT_ALERTS, AppConfig.getIosAutoAcceptAlerts())
        capabilities.setCapability(IOSMobileCapabilityType.AUTO_DISMISS_ALERTS, AppConfig.getIosAutoDismissAlerts())
        capabilities.setCapability(IOSMobileCapabilityType.SHOW_IOS_LOG, false)
        capabilities.setCapability("appium:autoLaunch", autoLaunch)

        val processArguments = HashMap<String, Array<String>>()
        capabilities.setCapability(IOSMobileCapabilityType.PROCESS_ARGUMENTS, processArguments)
        capabilities.setCapability("settings[customSnapshotTimeout]", 3)

        logger.info("DesiredCapabilities for iOS successfully built")
        return capabilities
    }
}
