package unit.app.driver

import app.appium.driver.IosDriver
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for [IosDriver].
 *
 * Tests the core functionality of the iOS driver:
 * - Creating an instance of IosDriver with different parameters
 * - Verifying the constructor
 *
 * Note: These tests do not start a real driver and do not connect to an Appium server.
 */
class IosDriverTest {

    /**
     * Tests creating an instance of IosDriver.
     */
    @Test
    fun `test ios driver instance creation`() {
        // Create an IosDriver instance with autoLaunch = true
        val iosDriver = IosDriver(true)

        // Verify that the IosDriver instance is created
        assertNotNull(iosDriver)
    }

    /**
     * Tests the IosDriver constructor with different autoLaunch values.
     */
    @Test
    fun `test constructor with different autoLaunch values`() {
        // Create IosDriver instances with different autoLaunch values
        val iosDriverWithAutoLaunchTrue = IosDriver(true)
        val iosDriverWithAutoLaunchFalse = IosDriver(false)

        // Verify that the instances are created
        assertNotNull(iosDriverWithAutoLaunchTrue)
        assertNotNull(iosDriverWithAutoLaunchFalse)
    }
}
