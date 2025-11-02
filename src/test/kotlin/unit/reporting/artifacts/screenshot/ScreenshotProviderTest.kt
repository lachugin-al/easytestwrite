package unit.reporting.artifacts.screenshot

import io.appium.java_client.AppiumDriver
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.openqa.selenium.OutputType
import reporting.artifacts.screenshot.AppiumScreenshotProvider
import kotlin.test.assertContentEquals

class ScreenshotProviderTest {

    @Test
    fun `AppiumScreenshotProvider delegates to driver getScreenshotAs BYTES`() {
        val driver = mockk<AppiumDriver<*>>()
        val bytes = byteArrayOf(1, 2, 3)
        every { driver.getScreenshotAs(OutputType.BYTES) } returns bytes

        val provider = AppiumScreenshotProvider(driver)
        val result = provider.getRawScreenshot()

        assertContentEquals(bytes.toList(), result.toList())
    }
}
