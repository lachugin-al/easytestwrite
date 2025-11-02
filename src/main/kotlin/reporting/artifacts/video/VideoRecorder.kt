package reporting.artifacts.video

import app.config.AppConfig
import app.model.Platform
import io.appium.java_client.AppiumDriver
import io.appium.java_client.MobileElement
import io.appium.java_client.screenrecording.CanRecordScreen
import io.qameta.allure.Allure
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

/**
 * Universal class for recording video during mobile test execution.
 *
 * Allows recording of test execution videos on Android and iOS devices.
 * Supports configuring video size, quality, and bitrate via configuration parameters.
 * Recorded videos are saved to the specified directory and can be attached to the Allure report.
 */
object VideoRecorder {
    private val logger = LoggerFactory.getLogger(VideoRecorder::class.java)

    private var currentVideoPath: String? = null
    private var isRecording = false

    /**
     * Starts video recording for the current test.
     *
     * Initializes recording using Appium commands depending on the platform (Android or iOS).
     * Creates a directory for saving the video if it does not exist,
     * and generates a unique file name based on the test name and timestamp.
     *
     * @param driver Instance of AppiumDriver to interact with the mobile device
     * @param testName Name of the test to be used in the video file name
     * @return True if recording started successfully, false otherwise
     */
    fun startRecording(driver: AppiumDriver<MobileElement>, testName: String): Boolean {
        if (!AppConfig.isVideoRecordingEnabled()) {
            val platform = AppConfig.getPlatform()
            logger.info("Video recording is disabled for platform $platform. To enable, use parameter ${platform.name.lowercase()}.video.recording.enabled=true")
            return false
        }

        if (isRecording) {
            logger.warn("Video recording is already in progress. Ignoring start request.")
            return false
        }

        try {
            logger.info("Starting video recording for test: $testName")
            val outputDir = File(AppConfig.getVideoRecordingOutputDir())
            if (!outputDir.exists()) outputDir.mkdirs()

            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val safeTestName = testName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            currentVideoPath = "${AppConfig.getVideoRecordingOutputDir()}/${safeTestName}_$timestamp.mp4"

            when (AppConfig.getPlatform()) {
                Platform.ANDROID -> {
                    // Use mobile:startMediaProjectionRecording for Android
                    val androidOptions = mapOf(
                        "videoSize" to AppConfig.getVideoRecordingSize(),
                        "videoQuality" to AppConfig.getVideoRecordingQuality().toString(),
                        "timeLimit" to "1800",
                        "forceRestart" to "true",
                        "bitRate" to AppConfig.getVideoRecordingBitrate().toString()
                    )
                    driver.executeScript("mobile:startMediaProjectionRecording", androidOptions)
                    logger.info(
                        "Video recording on Android started with size: ${AppConfig.getVideoRecordingSize()}, " +
                                "quality: ${AppConfig.getVideoRecordingQuality()}, " +
                                "bitrate: ${AppConfig.getVideoRecordingBitrate() / 1000} Kbps"
                    )
                }
                Platform.IOS -> {
                    // Use startRecordingScreen for iOS via CanRecordScreen interface
                    if (driver is CanRecordScreen) {
                        driver.startRecordingScreen()
                        logger.info("Video recording on iOS started via startRecordingScreen()")
                    } else {
                        logger.error("Driver does not support screen recording for iOS (does not implement CanRecordScreen)")
                        return false
                    }
                }
                else -> {
                    logger.error("Video recording is not supported for platform: ${AppConfig.getPlatform()}")
                    return false
                }
            }

            isRecording = true
            return true
        } catch (e: Exception) {
            logger.error("Failed to start video recording: ${e.message}", e)
            return false
        }
    }

    /**
     * Stops the current video recording and attaches it to the Allure report.
     *
     * Stops the recording, decodes the Base64-encoded content,
     * saves the video file to the specified directory,
     * and attaches it to the Allure report if required.
     * The file name is based on the test name and timestamp generated at start.
     *
     * @param driver Instance of AppiumDriver to interact with the mobile device
     * @param testName Name of the test (used for logging)
     * @param attachToAllure Flag indicating whether to attach the video to the Allure report (default true)
     * @return True if recording stopped and saved successfully, false otherwise
     */
    fun stopRecording(
        driver: AppiumDriver<MobileElement>,
        testName: String,
        attachToAllure: Boolean = true
    ): Boolean {
        if (!isRecording) {
            logger.warn("Video recording was not enabled in configuration.")
            return false
        }

        try {
            logger.info("Stopping video recording for test: $testName")

            val base64Video: String = when (AppConfig.getPlatform()) {
                Platform.ANDROID -> {
                    driver.executeScript("mobile:stopMediaProjectionRecording") as String
                }
                Platform.IOS -> {
                    if (driver is CanRecordScreen) {
                        driver.stopRecordingScreen()
                    } else {
                        logger.error("Driver does not support screen recording for iOS (does not implement CanRecordScreen)")
                        isRecording = false
                        return false
                    }
                }
                else -> {
                    logger.error("Stopping video recording is not supported for platform: ${AppConfig.getPlatform()}")
                    isRecording = false
                    return false
                }
            }

            isRecording = false

            // Decode and save video
            val videoBytes = Base64.getDecoder().decode(base64Video)
            val videoPath = currentVideoPath ?: "${AppConfig.getVideoRecordingOutputDir()}/unknown_test.mp4"
            Files.write(Paths.get(videoPath), videoBytes)
            logger.info("Video saved at: $videoPath")

            // Attach to Allure report if requested
            if (attachToAllure) {
                Allure.addAttachment(
                    "Test Recording",
                    "video/mp4",
                    ByteArrayInputStream(videoBytes),
                    "mp4"
                )
                logger.info("Video attached to Allure report")
            }

            return true
        } catch (e: Exception) {
            logger.error("Failed to stop video recording: ${e.message}", e)
            isRecording = false
            return false
        }
    }

    /**
     * Checks if video recording is enabled in the current configuration.
     *
     * Delegates to AppConfig to fetch the parameter for the current platform.
     * For Android and iOS, platform-specific parameters are used:
     * - android.video.recording.enabled
     * - ios.video.recording.enabled
     *
     * For other platforms, video recording is not supported and always returns false.
     *
     * @return True if video recording is enabled in configuration, false otherwise
     */
    fun isEnabled(): Boolean = AppConfig.isVideoRecordingEnabled()

    /**
     * Checks if a non-default video size is configured.
     *
     * Verifies whether the current value of video.recording.size differs from the default "1280x720".
     *
     * @return True if a custom video size is configured, false otherwise
     */
    fun isVideoSizeConfigured(): Boolean = AppConfig.getVideoRecordingSize() != "1280x720"

    /**
     * Checks if a non-default video quality is configured.
     *
     * Verifies whether the current value of video.recording.quality differs from the default 70.
     *
     * @return True if a custom video quality is configured, false otherwise
     */
    fun isVideoQualityConfigured(): Boolean = AppConfig.getVideoRecordingQuality() != 70
}
