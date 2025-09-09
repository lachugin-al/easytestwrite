package dsl.testing

import app.config.AppConfig
import app.model.Platform
import io.appium.java_client.AppiumDriver
import io.qameta.allure.Allure.addAttachment
import reporting.artifacts.screenshot.AppiumScreenshotProvider
import reporting.artifacts.screenshot.ImageProcessor
import reporting.artifacts.screenshot.ScreenshotProvider
import java.io.ByteArrayInputStream

/**
 * Base class for all testing contexts.
 *
 * Provides helper methods to execute code depending on the platform,
 * as well as to safely run optional actions without failing the test.
 *
 * @see TestingDslMarker
 */
@TestingDslMarker
abstract class BaseContext {

    /**
     * Driver provided by subclasses for taking screenshots.
     */
    protected abstract val driver: Any

    /**
     * Executes the given block only if the target platform is iOS.
     *
     * The [action] block will be ignored for all other platforms.
     *
     * @param action Lambda with actions to run on iOS.
     */
    fun onlyIos(action: () -> Unit) = action.invokeIfRequiredPlatform(Platform.IOS)

    /**
     * Executes the given block only if the target platform is Android.
     *
     * The [action] block will be ignored for all other platforms.
     *
     * @param action Lambda with actions to run on Android.
     */
    fun onlyAndroid(action: () -> Unit) = action.invokeIfRequiredPlatform(Platform.ANDROID)

    /**
     * Executes multiple blocks in a safe mode for the iOS platform only.
     *
     * Each block is executed independently of the result of the previous blocks.
     * If any block throws an error, it is swallowed and execution proceeds
     * to the next block. Blocks are executed only when the current platform is iOS.
     *
     * Example:
     * ```
     * optionalIos(
     *     { "Step 1" { action1() } },
     *     { "Step 2" { action2() } }
     * )
     * ```
     *
     * @param actions A set of lambdas with optional logic to execute.
     */
    @Suppress("SwallowedException")
    fun optionalIos(vararg actions: () -> Unit) {
        if (AppConfig.getPlatform() != Platform.IOS) return

        actions.forEach { action ->
            try {
                action.invoke()
            } catch (e: Exception) {
                // Errors are suppressed to continue executing subsequent steps
            }
        }
    }

    /**
     * Executes multiple blocks in a safe mode for the Android platform only.
     *
     * Each block is executed independently of the result of the previous blocks.
     * If any block throws an error, it is swallowed and execution proceeds
     * to the next block. Blocks are executed only when the current platform is Android.
     *
     * Example:
     * ```
     * optionalAndroid(
     *     { "Step 1" { action1() } },
     *     { "Step 2" { action2() } }
     * )
     * ```
     *
     * @param actions A set of lambdas with optional logic to execute.
     */
    @Suppress("SwallowedException")
    fun optionalAndroid(vararg actions: () -> Unit) {
        if (AppConfig.getPlatform() != Platform.ANDROID) return

        actions.forEach { action ->
            try {
                action.invoke()
            } catch (e: Exception) {
                // Errors are suppressed to continue executing subsequent steps
            }
        }
    }

    /**
     * Executes multiple blocks in a safe mode.
     *
     * Each block is executed independently of the result of the previous blocks.
     * If any block throws an error, it is swallowed and execution proceeds
     * to the next block.
     *
     * Example:
     * ```
     * optional(
     *     { step1() },
     *     { step2() },
     *     { step3() }
     * )
     * ```
     *
     * @param actions A set of lambdas with optional logic to execute.
     */
    @Suppress("SwallowedException")
    fun optional(vararg actions: () -> Unit) {
        actions.forEach { action ->
            try {
                action.invoke()
            } catch (e: Exception) {
                // Errors are suppressed to continue executing subsequent steps
            }
        }
    }

    /**
     * Takes a screenshot and attaches it to the Allure report.
     * Executed within {@link #optional}.
     *
     * @param name    Attachment name in the report.
     * @param scale   Image scale (0.1–1.0).
     * @param quality JPEG quality (1–100).
     */
    fun takeScreenshot(
        name: String,
        scale: Double = AppConfig.getScreenshotScale(),
        quality: Int = AppConfig.getScreenshotQuality()
    ) {
        optional(
            {
                val provider: ScreenshotProvider = when (driver) {
                    is AppiumDriver<*> -> AppiumScreenshotProvider(driver as AppiumDriver<*>)
                    else -> null
                } ?: return@optional

                val raw = provider.getRawScreenshot()
                val processed = ImageProcessor.processImage(raw, scale.coerceIn(0.1, 1.0), quality.coerceIn(1, 100))

                addAttachment(name, "image/jpeg", ByteArrayInputStream(processed), "jpg")
            }
        )
    }

    /**
     * Helper function to conditionally execute a block depending on the platform.
     *
     * @param platform The platform for which the code should run.
     */
    private fun (() -> Unit).invokeIfRequiredPlatform(platform: Platform) {
        if (AppConfig.getPlatform() == platform) invoke()
    }
}
