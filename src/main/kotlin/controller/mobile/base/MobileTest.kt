package controller.mobile.base

import app.App
import app.config.AppConfig
import app.appium.server.AppiumServerManager
import app.model.Platform
import controller.mobile.alert.AlertActions
import controller.mobile.core.AppContext
import controller.mobile.deeplink.DeeplinkOpener
import controller.mobile.events.EventDrivenUi
import controller.mobile.events.EventVerifier
import controller.mobile.interaction.UiClickActions
import controller.mobile.interaction.UiElementFinding
import controller.mobile.interaction.UiScrollGestures
import controller.mobile.interaction.UiTapActions
import controller.mobile.interaction.UiGetValueActions
import controller.mobile.interaction.UiTypingActions
import controller.mobile.interaction.UiVisibilityChecks
import controller.mobile.nativeactions.NativeActions
import dsl.testing.TestingContext
import events.EventStorage
import io.appium.java_client.MobileElement
import io.appium.java_client.android.AndroidDriver
import kotlinx.coroutines.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reporting.allure.AllureLogCapture
import reporting.artifacts.video.VideoRecorder
import org.junit.jupiter.api.TestInfo
import device.AnrWatcher
import device.EmulatorManager
import dsl.testing.SkipConditionExtension
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Base controller for mobile tests.
 *
 * The main class that contains the common infrastructure for working with Appium/Web drivers,
 * interacting with UI elements, sending user actions, verifying events in EventStorage,
 * as well as helper operations for scrolling and swiping on the screen.
 *
 * All test classes must extend this class.
 */
@ExtendWith(SkipConditionExtension::class)
open class MobileTest:
    AppContext,
    UiElementFinding,
    UiScrollGestures,
    UiTapActions,
    UiClickActions,
    UiGetValueActions,
    UiVisibilityChecks,
    UiTypingActions,
    EventVerifier,
    EventDrivenUi,
    DeeplinkOpener,
    NativeActions,
    AlertActions
{

    // ---- infrastructure and dependencies ----
    override val logger: Logger = LoggerFactory.getLogger(MobileTest::class.java)

    override lateinit var app: App
    protected lateinit var context: TestingContext

    // Event storage
    override val eventsFileStorage = EventStorage

    // CoroutineScope for managing coroutine lifecycle
    override val scope = CoroutineScope(Dispatchers.Default)
    override val jobs = mutableListOf<Deferred<*>>()

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MobileTest::class.java)
        private var emulatorStarted = false

        @BeforeAll
        @JvmStatic
        fun setUpAll() {
            // Start or adopt Appium server and begin monitoring
            try {
                AppiumServerManager.ensureStartedAndMonitored()
            } catch (e: Exception) {
                logger.error("Failed to start or detect Appium server: ${e.message}", e)
                throw e
            }

            // Check if emulator auto-start is enabled
            if (AppConfig.isEmulatorAutoStartEnabled()) {
                // Start emulator before all tests
                logger.info("Starting emulator before all tests")
                emulatorStarted = EmulatorManager.startEmulator()
                if (!emulatorStarted) {
                    logger.error("Failed to start emulator")
                }
            } else {
                logger.info("Emulator auto-start is disabled in settings")
            }
        }

        @AfterAll
        @JvmStatic
        fun tearDownAll() {
            // Stop Appium server (only if it was started by us)
            try {
                AppiumServerManager.shutdown()
            } catch (e: Exception) {
                logger.warn("Failed to stop Appium server cleanly: ${e.message}", e)
            }

            // Check if emulator auto-shutdown is enabled
            if (AppConfig.isEmulatorAutoShutdownEnabled()) {
                // Stop emulator after all tests
                logger.info("Stopping emulator after all tests")
                EmulatorManager.stopEmulator()
            } else {
                logger.info("Emulator auto-shutdown is disabled in settings")
            }
        }
    }

    // ---- lifecycle ----
    // Executed before each test
    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        // Clear logs before the test
        AllureLogCapture.clearLogs()
        // Initialize log capturing system
        AllureLogCapture.initialize()
        // Clear event storage before the test
        EventStorage.clear()

        // Check if emulator was successfully started, if required
        if (AppConfig.getPlatform() == Platform.ANDROID &&
            AppConfig.isEmulatorAutoStartEnabled() &&
            !emulatorStarted
        ) {
            logger.warn("Emulator was not successfully started in setUpAll(), attempting to start again")
            emulatorStarted = EmulatorManager.startEmulator()
            if (!emulatorStarted) {
                logger.error("Failed to start emulator before the test")
                throw RuntimeException("Failed to initialize Android driver. Make sure the emulator is running.")
            }
        }

        // Ensure Wi-Fi connectivity on Android emulator before each test
        if (AppConfig.getPlatform() == Platform.ANDROID) {
            val needEnsure = AppConfig.isEmulatorAutoStartEnabled() || (EmulatorManager.getEmulatorId() != null)
            if (needEnsure) {
                val wifiOk = EmulatorManager.ensureAndroidWifiConnectivity()
                if (!wifiOk) {
                    logger.error("Failed to ensure Wi-Fi connectivity on Android emulator before the test")
                    throw RuntimeException("Wi-Fi is not configured on the emulator. Stop the test or check the environment.")
                }
            }
        }

        // Initialize the application
        app = App().launch()
        context = TestingContext(driver)

        // Start video recording
        val testName = testInfo.displayName
        VideoRecorder.startRecording(driver, testName)

        // Start AnrWatcher for Android
        if (AppConfig.getPlatform() == Platform.ANDROID) {
            val androidDriver = driver as? AndroidDriver<MobileElement>
            androidDriver?.let { AnrWatcher.start(it) }
        }
    }

    // Executed after each test
    @AfterEach
    fun tearDown(testInfo: TestInfo) {
        // Wait for all event checks to complete
        awaitAllEventChecks()

        // Stop video recording
        val testName = testInfo.displayName
        VideoRecorder.stopRecording(driver, testName)

        // Stop AnrWatcher for Android
        if (AppConfig.getPlatform() == Platform.ANDROID) {
            AnrWatcher.stop()
        }

        // Attach logs to Allure report
        AllureLogCapture.attachLogsToAllureReport()

        // Close the application
        closeApp()
    }

    /**
     * Closes the application.
     * This method can be called from external classes, for example, from SkipConditionExtension,
     * when a test is skipped due to the @Skip annotation.
     */
    fun closeApp() {
        if (this::app.isInitialized) {
            app.close()
        } else {
            logger.debug("closeApp() called, but 'app' is not initialized; skipping close.")
        }
    }
}
