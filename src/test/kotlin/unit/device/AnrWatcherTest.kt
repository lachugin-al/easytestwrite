package unit.device

import device.AnrWatcher
import device.AnrWatcher.Clickable
import device.AnrWatcher.UiAutomatorDriver
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.NoSuchElementException

/**
 * Unit tests for [AnrWatcher].
 *
 * Verifies core behavior of the ANR watcher:
 * - Clicks "Wait" when an ANR dialog appears and the Wait button exists
 * - Clicks "Close app" when the Wait button is missing
 * - Calling start twice is safe and keeps a single watcher
 *
 * Note: UI strings inside the ANR dialog are intentionally kept in Russian
 * to match real device/system dialog texts and must not be changed.
 */
class AnrWatcherTest {

    private lateinit var driver: UiAutomatorDriver
    private lateinit var waitButton: Clickable
    private lateinit var closeButton: Clickable

    @BeforeEach
    fun setUp() {
        driver = mockk(relaxed = true)
        waitButton = mockk(relaxed = true)
        closeButton = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        AnrWatcher.stop()
        unmockkAll()
    }

    /**
     * Clicks "Wait" when an ANR dialog appears and the Wait button is present.
     */
    @Test
    fun `clicks Wait when ANR dialog appears and Wait button exists`() {
        every { driver.pageSource } returns "The application is not responding"
        every { driver.findElementByAndroidUIAutomator(match { it.contains("Wait") }) } returns waitButton

        AnrWatcher.start(driver, intervalMillis = 50)
        Thread.sleep(150)
        AnrWatcher.stop()

        verify(atLeast = 1) { waitButton.click() }
        verify(exactly = 0) { driver.findElementByAndroidUIAutomator(match { it.contains("Close app") }) }
    }

    /**
     * Clicks "Close app" when an ANR dialog appears but the Wait button is missing.
     */
    @Test
    fun `clicks Close when ANR dialog appears and Wait button is missing`() {
        every { driver.pageSource } returns "It looks like the application is not responding"
        every { driver.findElementByAndroidUIAutomator(match { it.contains("Wait") }) } throws NoSuchElementException("no wait")
        every { driver.findElementByAndroidUIAutomator(match { it.contains("Close app") }) } returns closeButton

        AnrWatcher.start(driver, intervalMillis = 50)
        Thread.sleep(150)
        AnrWatcher.stop()

        verify(atLeast = 1) { closeButton.click() }
    }

    /**
     * Calling start twice does not throw and maintains a single watcher instance.
     */
    @Test
    fun `start twice does not throw and keeps single watcher`() {
        every { driver.pageSource } returns ""

        assertDoesNotThrow {
            AnrWatcher.start(driver, intervalMillis = 50)
            AnrWatcher.start(driver, intervalMillis = 50)
        }
        Thread.sleep(100)
        AnrWatcher.stop()

        verify(exactly = 0) { driver.findElementByAndroidUIAutomator(any()) }
    }
}
