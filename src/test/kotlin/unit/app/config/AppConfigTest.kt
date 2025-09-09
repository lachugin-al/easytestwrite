package unit.app.config

import app.config.AppConfig
import app.model.Platform
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for [AppConfig].
 *
 * Tests the core functionality of the configuration class:
 * - Loading configuration
 * - Retrieving parameters for different platforms
 * - Verifying default values
 */
class AppConfigTest {

    /**
     * Tests retrieval of the current platform.
     */
    @Test
    fun `test get platform`() {
        // Get the current platform
        val platform = AppConfig.getPlatform()

        // Verify the platform is not null and is one of the allowed values
        assertNotNull(platform)
        assertTrue(platform in Platform.values())
    }

    /**
     * Tests helper methods for checking the current platform.
     */
    @Test
    fun `test platform check methods`() {
        // Get the current platform
        val currentPlatform = AppConfig.getPlatform()

        // Verify consistency of platform-detection methods
        when (currentPlatform) {
            Platform.ANDROID -> {
                assertTrue(AppConfig.isAndroid())
                assertFalse(AppConfig.isiOS())
            }

            Platform.IOS -> {
                assertFalse(AppConfig.isAndroid())
                assertTrue(AppConfig.isiOS())
            }
        }

        // Ensure getPlatform returns the expected platform
        assertEquals(currentPlatform, AppConfig.getPlatform())
    }

    /**
     * Tests retrieval of the Appium server URL.
     */
    @Test
    fun `test get appium url`() {
        // Get the Appium server URL
        val appiumUrl = AppConfig.getAppiumUrl()

        // Verify the URL is not null and has the correct format
        assertNotNull(appiumUrl)
        assertTrue(appiumUrl.toString().startsWith("http://"))
    }

    /**
     * Tests retrieval of the app name for the current platform.
     */
    @Test
    fun `test get app name for current platform`() {
        // Get the current platform
        val currentPlatform = AppConfig.getPlatform()

        // Get the app name
        val appName = AppConfig.getAppName()

        // Verify the app name depending on the current platform
        when (currentPlatform) {
            Platform.ANDROID, Platform.IOS -> {
                assertNotNull(appName)
                assertFalse(appName.isEmpty())
            }
        }
    }

    /**
     * Tests retrieval of video recording settings depending on the platform.
     */
    @Test
    fun `test video recording settings`() {
        // Check video recording settings
        val isEnabled = AppConfig.isVideoRecordingEnabled()
        val size = AppConfig.getVideoRecordingSize()
        val quality = AppConfig.getVideoRecordingQuality()
        val bitrate = AppConfig.getVideoRecordingBitrate()
        val outputDir = AppConfig.getVideoRecordingOutputDir()

        // Verify all values are not null and have the expected format
        assertNotNull(size)
        assertTrue(size.matches(Regex("\\d+x\\d+")))
        assertTrue(quality in 0..100)
        assertTrue(bitrate > 0)
        assertNotNull(outputDir)
        assertFalse(outputDir.isEmpty())
    }

    /**
     * Helper method to set a private field's value.
     */
    private fun setPrivateField(clazz: Class<*>, fieldName: String, value: Any) {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(null, value)
    }
}
