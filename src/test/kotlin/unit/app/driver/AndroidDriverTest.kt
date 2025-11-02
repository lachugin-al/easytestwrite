package unit.app.driver

import app.appium.driver.AndroidDriver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for [AndroidDriver].
 *
 * Tests the core functionality of the Android driver:
 * - Creating an instance of AndroidDriver with different parameters
 * - Verifying the constructor
 *
 * Note: These tests do not start a real driver and do not connect to an Appium server.
 */
class AndroidDriverTest {

    /**
     * Tests creating an instance of AndroidDriver.
     */
    @Test
    fun `test android driver instance creation`() {
        // Create an AndroidDriver instance with autoLaunch = true
        val androidDriver = AndroidDriver(true)

        // Verify that the AndroidDriver instance is created
        assertNotNull(androidDriver)
    }

    /**
     * Tests the AndroidDriver constructor with different autoLaunch values.
     */
    @Test
    fun `test constructor with different autoLaunch values`() {
        // Create AndroidDriver instances with different autoLaunch values
        val androidDriverWithAutoLaunchTrue = AndroidDriver(true)
        val androidDriverWithAutoLaunchFalse = AndroidDriver(false)

        // Verify that the instances are created
        assertNotNull(androidDriverWithAutoLaunchTrue)
        assertNotNull(androidDriverWithAutoLaunchFalse)
    }
}
