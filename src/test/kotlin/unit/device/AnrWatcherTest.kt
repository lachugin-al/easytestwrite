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

    @Test
    fun `clicks Wait when ANR dialog appears and Wait button exists`() {
        every { driver.pageSource } returns "Приложение не отвечает"
        every { driver.findElementByAndroidUIAutomator(match { it.contains("Подождать") }) } returns waitButton

        AnrWatcher.start(driver, intervalMillis = 50)
        Thread.sleep(150) // give the coroutine some time
        AnrWatcher.stop()

        verify(atLeast = 1) { waitButton.click() }
        // Ensure it didn't try Close path
        verify(exactly = 0) { driver.findElementByAndroidUIAutomator(match { it.contains("Закрыть приложение") }) }
    }

    @Test
    fun `clicks Close when ANR dialog appears and Wait button is missing`() {
        every { driver.pageSource } returns "Похоже, приложение не отвечает"
        every { driver.findElementByAndroidUIAutomator(match { it.contains("Подождать") }) } throws NoSuchElementException("no wait")
        every { driver.findElementByAndroidUIAutomator(match { it.contains("Закрыть приложение") }) } returns closeButton

        AnrWatcher.start(driver, intervalMillis = 50)
        Thread.sleep(150)
        AnrWatcher.stop()

        verify(atLeast = 1) { closeButton.click() }
    }

    @Test
    fun `start twice does not throw and keeps single watcher`() {
        every { driver.pageSource } returns ""

        assertDoesNotThrow {
            AnrWatcher.start(driver, intervalMillis = 50)
            AnrWatcher.start(driver, intervalMillis = 50)
        }
        Thread.sleep(100)
        AnrWatcher.stop()

        // Since no ANR text, it should not try to find any buttons
        verify(exactly = 0) { driver.findElementByAndroidUIAutomator(any()) }
    }
}
