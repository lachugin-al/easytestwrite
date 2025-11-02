package reporting.artifacts.screenshot

import io.appium.java_client.AppiumDriver
import org.openqa.selenium.OutputType

/**
 * Interface for obtaining raw screenshot bytes.
 */
interface ScreenshotProvider {
    fun getRawScreenshot(): ByteArray
}

/** Provider for AppiumDriver. */
class AppiumScreenshotProvider(
    private val driver: AppiumDriver<*>
) : ScreenshotProvider {
    override fun getRawScreenshot(): ByteArray =
        driver.getScreenshotAs(OutputType.BYTES)
}
