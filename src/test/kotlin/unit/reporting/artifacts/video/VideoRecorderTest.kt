package unit.reporting.artifacts.video

import app.config.AppConfig
import app.model.Platform
import io.appium.java_client.AppiumDriver
import io.appium.java_client.MobileElement
import io.mockk.*
import io.qameta.allure.Allure
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import reporting.artifacts.video.VideoRecorder
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoRecorderTest {

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        mockkObject(AppConfig)
        mockkStatic(Allure::class)
    }

    @AfterEach
    fun tearDown() {
        runCatching {
            val cls = VideoRecorder::class.java
            val isRecordingField = cls.getDeclaredField("isRecording").apply { isAccessible = true }
            val currentVideoPathField = cls.getDeclaredField("currentVideoPath").apply { isAccessible = true }
            val instance = VideoRecorder
            isRecordingField.set(instance, false)
            currentVideoPathField.set(instance, null)
        }
        unmockkAll()
    }

    private fun configureCommonAndroid(enabled: Boolean = true) {
        every { AppConfig.getPlatform() } returns Platform.ANDROID
        every { AppConfig.isVideoRecordingEnabled() } returns enabled
        every { AppConfig.getVideoRecordingSize() } returns "640x360"
        every { AppConfig.getVideoRecordingQuality() } returns 70
        every { AppConfig.getVideoRecordingBitrate() } returns 100_000
        every { AppConfig.getVideoRecordingOutputDir() } returns tempDir.toString()
    }

    @Test
    fun `isEnabled and config helpers reflect AppConfig`() {
        every { AppConfig.isVideoRecordingEnabled() } returns true
        assertTrue(VideoRecorder.isEnabled())
        every { AppConfig.isVideoRecordingEnabled() } returns false
        assertFalse(VideoRecorder.isEnabled())

        every { AppConfig.getVideoRecordingSize() } returns "1280x720"
        assertFalse(VideoRecorder.isVideoSizeConfigured())
        every { AppConfig.getVideoRecordingSize() } returns "640x360"
        assertTrue(VideoRecorder.isVideoSizeConfigured())

        every { AppConfig.getVideoRecordingQuality() } returns 70
        assertFalse(VideoRecorder.isVideoQualityConfigured())
        every { AppConfig.getVideoRecordingQuality() } returns 60
        assertTrue(VideoRecorder.isVideoQualityConfigured())
    }

    @Test
    fun `startRecording Android happy path`() {
        configureCommonAndroid(enabled = true)
        val driver = mockk<AppiumDriver<MobileElement>>(relaxed = true)

        val ok = VideoRecorder.startRecording(driver, "Test Name")
        assertTrue(ok)
        verify { driver.executeScript(eq("mobile:startMediaProjectionRecording"), any<Map<String, String>>()) }
    }

    @Test
    fun `startRecording returns false when disabled`() {
        configureCommonAndroid(enabled = false)
        val driver = mockk<AppiumDriver<MobileElement>>(relaxed = true)
        val ok = VideoRecorder.startRecording(driver, "DisabledTest")
        assertFalse(ok)
        verify(exactly = 0) { driver.executeScript(any<String>(), any()) }
    }

    @Test
    fun `double start returns false`() {
        configureCommonAndroid(enabled = true)
        val driver = mockk<AppiumDriver<MobileElement>>(relaxed = true)
        assertTrue(VideoRecorder.startRecording(driver, "DoubleStart"))
        assertFalse(VideoRecorder.startRecording(driver, "DoubleStart"))
    }

    @Test
    fun `stopRecording returns false if not recording`() {
        configureCommonAndroid(enabled = true)
        val driver = mockk<AppiumDriver<MobileElement>>(relaxed = true)
        assertFalse(VideoRecorder.stopRecording(driver, "NoStart"))
    }

    @Test
    fun `full Android flow saves file and attaches to Allure`() {
        configureCommonAndroid(enabled = true)
        val driver = mockk<AppiumDriver<MobileElement>>(relaxed = true)
        assertTrue(VideoRecorder.startRecording(driver, "Android Flow"))

        val bytes = byteArrayOf(0x00, 0x01, 0x02)
        val base64 = Base64.getEncoder().encodeToString(bytes)
        every { driver.executeScript("mobile:stopMediaProjectionRecording") } returns base64

        val result = VideoRecorder.stopRecording(driver, "Android Flow", attachToAllure = true)
        assertTrue(result)

        verify { Allure.addAttachment("Запись теста", "video/mp4", any<java.io.InputStream>(), "mp4") }
        val files = Files.list(tempDir).use { it.toList() }
        assertTrue(files.any { it.fileName.toString().endsWith(".mp4") })
    }

    @Test
    fun `Android stop without Allure attachment`() {
        configureCommonAndroid(enabled = true)
        val driver = mockk<AppiumDriver<MobileElement>>(relaxed = true)
        assertTrue(VideoRecorder.startRecording(driver, "Android Flow 2"))

        val bytes = byteArrayOf(0x03, 0x04)
        val base64 = Base64.getEncoder().encodeToString(bytes)
        every { driver.executeScript("mobile:stopMediaProjectionRecording") } returns base64

        val result = VideoRecorder.stopRecording(driver, "Android Flow 2", attachToAllure = false)
        assertTrue(result)
        verify(exactly = 0) { Allure.addAttachment(any(), any(), any<java.io.InputStream>(), any()) }
        val files = Files.list(tempDir).use { it.toList() }
        assertTrue(files.any { it.fileName.toString().endsWith(".mp4") })
    }

    @Test
    fun `iOS start returns false when driver cannot record screen`() {
        every { AppConfig.getPlatform() } returns Platform.IOS
        every { AppConfig.isVideoRecordingEnabled() } returns true
        every { AppConfig.getVideoRecordingOutputDir() } returns tempDir.toString()
        val driver = mockk<AppiumDriver<MobileElement>>(relaxed = true)
        assertFalse(VideoRecorder.startRecording(driver, "IOS Test"))
    }
}
