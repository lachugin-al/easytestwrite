package app

import app.config.AppConfig
import app.driver.AndroidDriver
import app.driver.IosDriver
import app.model.Platform
import io.appium.java_client.AppiumDriver
import io.appium.java_client.MobileElement
import org.openqa.selenium.WebDriverException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import proxy.ProxyInspector
import proxy.WebServer

/**
 * Main controller class for initializing the test environment infrastructure.
 *
 * Within a single instance of [App], it manages the lifecycle of:
 * - Appium driver for Android/iOS
 * - Playwright web driver for web testing
 * - Local [WebServer] for handling auxiliary requests
 *
 * After test execution, all related resources are properly released.
 */
class App() : AutoCloseable {
    companion object {
        /** The currently running application instance. */
        lateinit var current: App
    }

    init {
        // Save the reference when created
        current = this
    }

    private val logger: Logger = LoggerFactory.getLogger(App::class.java)

    /** Instance of the Appium driver for mobile testing (Android/iOS). */
    var driver: AppiumDriver<MobileElement>? = null
        private set

    /** Local web server used for auxiliary purposes in tests. */
    val webServer = WebServer()

    /**
     * Performs full environment initialization:
     * - Creates a new driver for the selected platform
     * - Starts the local [WebServer]
     *
     * If a previous driver already exists — it is properly closed first.
     *
     * @return current [App] instance for convenient call chaining
     */
    fun launch(): App {
        driver?.let {
            close()
        }

        createDriver()
        webServer.start()

        return this
    }

    /**
     * Creates a new driver instance depending on the platform specified in [app.config.AppConfig].
     *
     * Supported platforms:
     * - Android (AppiumDriver)
     * - iOS (AppiumDriver)
     * - Web (Playwright Page)
     */
    private fun createDriver() {
        when (AppConfig.getPlatform()) {
            Platform.ANDROID -> {
                logger.info("Initializing Android driver")
                driver = AndroidDriver(autoLaunch = true).getAndroidDriver(3)
                // After the session starts, print emulator proxy settings
                try {
                    ProxyInspector.logAndroidProxy()
                } catch (e: Exception) {
                    logger.warn("Failed to print Android proxy settings: ${e.message}")
                }
            }

            Platform.IOS -> {
                logger.info("Initializing iOS driver")
                driver = IosDriver(autoLaunch = true).getIOSDriver(3)
                // After the session starts, print simulator (macOS) proxy settings
                try {
                    ProxyInspector.logIosSimulatorProxy()
                } catch (e: Exception) {
                    logger.warn("Failed to print iOS proxy settings: ${e.message}")
                }
            }
        }
    }

    /**
     * Properly shuts down all active components:
     * - Terminates the mobile application (if driver was initialized)
     * - Stops the local web server
     * - Closes the Playwright page for web tests
     *
     * All exceptions during resource shutdown are logged and suppressed to prevent process interruption.
     */
    override fun close() {
        when (AppConfig.getPlatform()) {
            Platform.ANDROID -> {
                driver?.let {
                    try {
                        it.terminateApp(AppConfig.getAppPackage())
                        it.quit()
                    } catch (e: WebDriverException) {
                        logger.error("Error while closing Appium driver session on Android", e)
                    } finally {
                        driver = null
                    }
                }
            }

            Platform.IOS -> {
                driver?.let {
                    try {
                        it.terminateApp(AppConfig.getBundleId())
                        it.quit()
                    } catch (e: WebDriverException) {
                        logger.error("Error while closing Appium driver session on iOS", e)
                    } finally {
                        driver = null
                    }
                }
            }
        }

        // Always close the WebServer regardless of platform
        try {
            webServer.close()
        } catch (e: Exception) {
            logger.error("Error while closing WebServer: ${e.message}", e)
        }
    }
}
