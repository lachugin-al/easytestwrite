package controller.mobile.alert

import io.appium.java_client.AppiumDriver
import io.appium.java_client.MobileElement
import org.openqa.selenium.NoAlertPresentException
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

/**
 * Wrapper class for working with system (native) alerts on Android and iOS devices.
 *
 * Provides methods for:
 * - Checking if an alert is present;
 * - Accepting an alert;
 * - Dismissing an alert;
 * - Retrieving alert text.
 *
 * Uses WebDriverWait with a specified timeout for waiting for alerts.
 *
 * @property driver AppiumDriver that controls the mobile device.
 * @property timeoutExpectation Timeout in seconds for waiting for an alert.
 * @property pollingInterval Polling interval in milliseconds.
 */
class AlertHandler(
    private val driver: AppiumDriver<MobileElement>,
    private val timeoutExpectation: Long,
    private val pollingInterval: Long,
) {

    /**
     * Checks if a system alert is currently displayed.
     *
     * @return true if an alert is found within [timeout]; false otherwise or if an error occurs.
     */
    fun isAlertPresent(): Boolean {
        return try {
            WebDriverWait(driver, timeoutExpectation, pollingInterval)
                .until(ExpectedConditions.alertIsPresent())
            true
        } catch (e: NoAlertPresentException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Accepts the currently displayed system alert.
     *
     * Used, for example, to allow access to geolocation, camera, etc.
     *
     * @throws NoAlertPresentException if the alert is not displayed within [timeout] seconds.
     */
    fun accept(): Unit {
        val wait = WebDriverWait(driver, timeoutExpectation, pollingInterval)
        val alert = wait.until(ExpectedConditions.alertIsPresent())
        alert.accept()
    }

    /**
     * Dismisses the currently displayed system alert.
     *
     * Can be used if the user needs to deny permissions.
     *
     * @throws NoAlertPresentException if the alert is not displayed within [timeout] seconds.
     */
    fun dismiss(): Unit {
        val wait = WebDriverWait(driver, timeoutExpectation, pollingInterval)
        val alert = wait.until(ExpectedConditions.alertIsPresent())
        alert.dismiss()
    }

    /**
     * Returns the text of the currently displayed system alert.
     *
     * @return The text displayed in the alert body (e.g., "Allow the app to track your activity")
     * @throws NoAlertPresentException if the alert is not displayed within [timeout] seconds.
     */
    fun getText(): String {
        val wait = WebDriverWait(driver, timeoutExpectation, pollingInterval)
        return wait.until(ExpectedConditions.alertIsPresent()).text
    }
}
