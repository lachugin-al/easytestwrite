package utils.screenshot

import com.microsoft.playwright.Page
import io.appium.java_client.AppiumDriver
import org.openqa.selenium.OutputType

/**
 * Интерфейс для получения «сырых» байтов скриншота.
 */
interface ScreenshotProvider {
    fun getRawScreenshot(): ByteArray
}

/** Провайдер для AppiumDriver. */
class AppiumScreenshotProvider(
    private val driver: AppiumDriver<*>
) : ScreenshotProvider {
    override fun getRawScreenshot(): ByteArray =
        driver.getScreenshotAs(OutputType.BYTES)
}

/** Провайдер для Playwright Page. */
class PlaywrightScreenshotProvider(
    private val page: Page
) : ScreenshotProvider {
    override fun getRawScreenshot(): ByteArray =
        page.screenshot()
}
