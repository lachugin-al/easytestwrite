package device

import io.appium.java_client.MobileElement
import io.appium.java_client.android.AndroidDriver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.openqa.selenium.NoSuchElementException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Utility for monitoring and automatically handling ANR (Application Not Responding) dialogs in Android.
 *
 * ANR Watcher runs in a separate coroutine and periodically checks for ANR dialogs.
 * When a dialog is detected, it automatically presses the "Wait" or "Close" button.
 *
 * It is recommended to start ANR Watcher once at the beginning of all tests using the @BeforeAll annotation.
 */
object AnrWatcher {
    private val logger: Logger = LoggerFactory.getLogger(AnrWatcher::class.java)
    private var job: Job? = null

    interface Clickable { fun click() }
    interface UiAutomatorDriver {
        val pageSource: String
        fun findElementByAndroidUIAutomator(selector: String): Clickable
    }

    /**
     * Starts ANR Watcher for monitoring ANR dialogs (universal version with minimal contract).
     */
    fun start(driver: UiAutomatorDriver, intervalMillis: Long = 2000L) {
        if (job != null) {
            logger.info("ANR Watcher is already running")
            return  // already running
        }

        logger.info("Starting ANR Watcher with interval $intervalMillis ms")
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val pageSource = driver.pageSource
                    if (pageSource.contains("is not responding", ignoreCase = true)) {
                        logger.info("ANR dialog detected. Attempting to press 'Wait' or 'Close'.")

                        try {
                            val waitButton = driver.findElementByAndroidUIAutomator(
                                "new UiSelector().textContains(\"Wait\")"
                            )
                            waitButton.click()
                            logger.info("'Wait' button pressed")
                        } catch (e: NoSuchElementException) {
                            try {
                                val closeButton = driver.findElementByAndroidUIAutomator(
                                    "new UiSelector().textContains(\"Close app\")"
                                )
                                closeButton.click()
                                logger.info("'Close' button pressed")
                            } catch (ignored: NoSuchElementException) {
                                logger.warn("Failed to find 'Wait' or 'Close' buttons")
                            }
                        }
                    }

                    delay(intervalMillis)
                } catch (e: CancellationException) {
                    // Ignore CancellationException, as this is expected when the coroutine is cancelled
                    logger.debug("ANR Watcher coroutine was cancelled: ${e.message}")
                } catch (e: Exception) {
                    logger.error("Error in ANR Watcher coroutine: ${e.message}", e)
                    delay(intervalMillis)
                }
            }
        }
    }

    /**
     * Starts ANR Watcher for AndroidDriver (adapter for the universal contract).
     */
    fun start(driver: AndroidDriver<MobileElement>, intervalMillis: Long = 2000L) {
        val adapter = object : UiAutomatorDriver {
            override val pageSource: String get() = driver.pageSource
            override fun findElementByAndroidUIAutomator(selector: String): Clickable {
                val element = driver.findElementByAndroidUIAutomator(selector)
                return object : Clickable { override fun click() = element.click() }
            }
        }
        start(adapter, intervalMillis)
    }

    /**
     * Stops ANR Watcher.
     */
    fun stop() {
        try {
            job?.cancel()
            job = null
            logger.info("ANR Watcher stopped")
        } catch (e: Exception) {
            // Ignore JobCancellationException, as this is expected when the coroutine is cancelled
            logger.debug("Exception while stopping ANR Watcher: ${e.message}")
        }
    }
}
