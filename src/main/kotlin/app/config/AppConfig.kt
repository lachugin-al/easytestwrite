package app.config

import app.model.Platform
import io.github.cdimascio.dotenv.Dotenv
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URL
import java.util.Properties

/**
 * Configuration class for the test framework.
 *
 * Loads settings from JVM system properties (–D). If not provided, loads them from `config.properties`.
 * Provides typed access to test run parameters, including:
 * - Target platform (Android, iOS, Web)
 * - Appium server connection settings
 * - Device and application details
 * - Playwright configuration for web tests
 *
 * Configuration is loaded on the first access to [AppConfig].
 *
 * @throws IllegalArgumentException if the `config.properties` file is not found
 */
object AppConfig {
    private val logger: Logger = LoggerFactory.getLogger(AppConfig::class.java)

    // .env loader
    private val dotenv: Dotenv? = runCatching {
        Dotenv.configure()
            .ignoreIfMissing()     // if .env is missing — it’s fine
            .ignoreIfMalformed()   // skip malformed lines
            .load()
    }.getOrNull()

    // Transform "appium.url" -> "APPIUM_URL" and look it up in .env
    private fun envGet(key: String): String? {
        val k1 = key.uppercase().replace('.', '_')
        val k2 = key
        return dotenv?.get(k1)
            ?: dotenv?.get(k2)
            ?: System.getenv(k1)
            ?: System.getenv(k2)
    }

    private val properties = Properties().apply {
        try {
            val resourceStream = Thread.currentThread()
                .contextClassLoader
                .getResourceAsStream("config.properties")
                ?: throw IllegalArgumentException(
                    "File config.properties not found in test/resources directory"
                )
            load(resourceStream)
            logger.info("Configuration successfully loaded from config.properties")
        } catch (e: Exception) {
            logger.error("Failed to load config.properties: ${e.message}", e)
            throw e
        }
    }

    // Priority -> System.getProperty > .env/ENV > config.properties > default
    private fun prop(name: String, default: String): String =
        System.getProperty(name)
            ?: envGet(name)
            ?: properties.getProperty(name, default)

    private fun propBoolean(name: String, default: Boolean): Boolean =
        System.getProperty(name)?.toBooleanStrictOrNull()
            ?: envGet(name)?.toBooleanStrictOrNull()
            ?: properties.getProperty(name)?.toBooleanStrictOrNull()
            ?: default

    // Appium URL
    private val appiumUrl: URL = URI(prop("appium.url", "http://localhost:4723/")).toURL()

    // Target platform
    private val platform: Platform = runCatching {
        Platform.valueOf(prop("platform", "ANDROID"))
    }.getOrElse { Platform.ANDROID }

    // OS versions
    private val androidVersion: String = prop("android.version", "16")
    private val iosVersion: String = prop("ios.version", "18.4")

    // Device names
    private val androidDeviceName: String = prop("android.device.name", "WBA16")
    private val iosDeviceName: String = prop("ios.device.name", "iPhone 16 Plus")

    // Application paths
    private val androidAppName: String = prop("android.app.name", "android.apk")
    private val iosAppName: String = prop("ios.app.name", "ios.app")

    // Android activity and package
    private val appActivity: String = prop(
        "app.activity",
        "MainActivity"
    )
    private val appPackage: String = prop(
        "app.package",
        "com.dev"
    )
    private val bundleId: String = prop(
        "bundle.id",
        "MOBILEAPP.DEV"
    )

    // iOS alerts configuration - both default to false
    private val iosAutoAcceptAlerts: Boolean = propBoolean("ios.auto_accept_alerts", false)
    private val iosAutoDismissAlerts: Boolean = propBoolean("ios.auto_dismiss_alerts", false)

    // Android headless mode
    private val androidHeadlessMode: Boolean = propBoolean("android.headless.mode", true)

    // Video recording settings
    private val androidVideoRecordingEnabled: Boolean = propBoolean("android.video.recording.enabled", false)
    private val iosVideoRecordingEnabled: Boolean = propBoolean("ios.video.recording.enabled", false)
    private val videoRecordingSize: String = prop("video.recording.size", "640x360")
    private val videoRecordingQuality: Int = prop("video.recording.quality", "70").toInt()
    private val videoRecordingBitrate: Int = prop("video.recording.bitrate", "100000").toInt()
    private val videoRecordingOutputDir: String = prop("video.recording.output.dir", "build/videos")

    // Emulator/simulator auto-start and auto-shutdown
    private val emulatorAutoStart: Boolean = propBoolean("emulator.auto.start", true)
    private val emulatorAutoShutdown: Boolean = propBoolean("emulator.auto.shutdown", true)

    // Screenshot scaling applied when preparing artifacts.
    private val screenshotScale: Double =
        prop("screenshot.scale", "0.5").toDoubleOrNull()
            ?.coerceIn(0.1, 1.0) ?: 0.5

    // JPEG quality when saving screenshots.
    private val screenshotQuality: Int =
        prop("screenshot.quality", "100").toIntOrNull()
            ?.coerceIn(1, 100) ?: 100

    // API

    /**
     * @return true if the current platform is Android.
     */
    fun isAndroid(): Boolean = platform == Platform.ANDROID

    /**
     * @return true if the current platform is iOS.
     */
    fun isiOS(): Boolean = platform == Platform.IOS

    /**
     * @return The current target platform for tests.
     */
    fun getPlatform(): Platform = platform

    /**
     * @return The Appium server URL.
     */
    fun getAppiumUrl(): URL = appiumUrl

    /**
     * @return The Android version for the test environment.
     */
    fun getAndroidVersion(): String = androidVersion

    /**
     * @return The iOS version for the test environment.
     */
    fun getIosVersion(): String = iosVersion

    /**
     * @return The name of the Android device used for tests.
     */
    fun getAndroidDeviceName(): String = androidDeviceName

    /**
     * @return The name of the iOS device used for tests.
     */
    fun getIosDeviceName(): String = iosDeviceName

    /**
     * @return The main activity of the Android app to launch.
     */
    fun getAppActivity(): String = appActivity

    /**
     * @return The Android app package name.
     */
    fun getAppPackage(): String = appPackage

    /**
     * @return The bundle ID of the iOS app.
     */
    fun getBundleId(): String = bundleId

    /**
     * @return The value for IOSMobileCapabilityType.AUTO_ACCEPT_ALERTS.
     * Defaults to false if not specified in the configuration.
     */
    fun getIosAutoAcceptAlerts(): Boolean = iosAutoAcceptAlerts

    /**
     * @return The value for IOSMobileCapabilityType.AUTO_DISMISS_ALERTS.
     * Defaults to false if not specified in the configuration.
     */
    fun getIosAutoDismissAlerts(): Boolean = iosAutoDismissAlerts

    /**
     * @return The name of the APK or .app file depending on the platform.
     * Returns an empty string for Web.
     */
    fun getAppName(): String {
        return when (platform) {
            Platform.ANDROID -> androidAppName
            Platform.IOS -> iosAppName
        }
    }

    /**
     * @return true if the Android emulator runs in headless mode (without UI).
     */
    fun isAndroidHeadlessMode(): Boolean = androidHeadlessMode

    /**
     * Checks if video recording is enabled for the current platform.
     *
     * For Android and iOS, platform-specific parameters are used:
     * - android.video.recording.enabled
     * - ios.video.recording.enabled
     *
     * For other platforms, video recording is not supported and always returns false.
     *
     * @return true if video recording is enabled for the current platform.
     */
    fun isVideoRecordingEnabled(): Boolean = when (platform) {
        Platform.ANDROID -> androidVideoRecordingEnabled
        Platform.IOS -> iosVideoRecordingEnabled
        else -> false
    }

    /**
     * @return The video recording resolution (e.g., "1280x720").
     */
    fun getVideoRecordingSize(): String = videoRecordingSize

    /**
     * @return The video recording quality (0-100).
     */
    fun getVideoRecordingQuality(): Int = videoRecordingQuality

    /**
     * @return The output directory for video recordings.
     */
    fun getVideoRecordingOutputDir(): String = videoRecordingOutputDir

    /**
     * @return The video recording bitrate (bits per second).
     */
    fun getVideoRecordingBitrate(): Int = videoRecordingBitrate

    /**
     * Checks if automatic start of emulator/simulator is enabled.
     *
     * @return true if auto-start is enabled.
     */
    fun isEmulatorAutoStartEnabled(): Boolean = emulatorAutoStart

    /**
     * Checks if automatic shutdown of emulator/simulator is enabled.
     *
     * @return true if auto-shutdown is enabled.
     */
    fun isEmulatorAutoShutdownEnabled(): Boolean = emulatorAutoShutdown

    /**
     * Returns the effective screenshot scale.
     *
     * Value is already clamped to **0.1..1.0**.
     * Defaults to **0.5** if not specified in the configuration.
     *
     * @return screenshot scale (Double)
     */
    fun getScreenshotScale(): Double = screenshotScale

    /**
     * Returns the JPEG quality for screenshots.
     *
     * Value is already clamped to **1..100**.
     * Defaults to **100** if not specified in the configuration.
     *
     * @return JPEG quality (Int)
     */
    fun getScreenshotQuality(): Int = screenshotQuality
}
