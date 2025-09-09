package controller.mobile.nativeactions

import app.config.AppConfig
import app.model.Platform
import controller.mobile.core.AppContext
import dsl.testing.StepContext
import io.appium.java_client.AppiumDriver
import io.appium.java_client.MobileElement
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.nativekey.AndroidKey
import io.appium.java_client.android.nativekey.KeyEvent
import io.appium.java_client.ios.IOSDriver

interface NativeActions : AppContext {

    /**
     * Universal method for sending native commands (e.g., key presses) to a mobile device.
     *
     * Use this method to send platform-dependent commands in test scenarios.
     *
     * @param androidKey Android key (e.g., AndroidKey.BACK, AndroidKey.ENTER).
     *                   Passed only for the Android platform; ignored for others.
     * @param iosKey String code of the iOS key (e.g., "\n" for Enter).
     *               Passed only for iOS; ignored for others.
     *
     * @throws IllegalArgumentException if the required parameter is not provided for the current platform.
     * @throws UnsupportedOperationException if the method is called for an unsupported platform (e.g., Web).
     */
    fun StepContext.performNativeAction(
        androidKey: AndroidKey? = null,
        iosKey: String? = null
    ) {
        when (AppConfig.getPlatform()) {
            Platform.ANDROID -> {
                val androidDriver = driver as? AndroidDriver ?: error("Driver is not AndroidDriver")
                if (androidKey != null) {
                    androidDriver.pressKey(KeyEvent(androidKey))
                } else {
                    throw IllegalArgumentException("Parameter androidKey must be provided for Android platform")
                }
            }

            Platform.IOS -> {
                val iosDriver = driver as? IOSDriver ?: error("Driver is not IOSDriver")
                if (iosKey != null) {
                    iosDriver.keyboard.pressKey(iosKey)
                } else {
                    throw IllegalArgumentException("Parameter iosKey must be provided for iOS platform")
                }
            }
        }
    }

    /**
     * Performs a native Enter (or Return) key press on a mobile device.
     *
     * Uses platform-dependent keys for Android and iOS:
     * - Android: AndroidKey.ENTER
     * - iOS: "\n"
     *
     * @throws IllegalArgumentException if the platform is not supported or the driver is not initialized.
     */
    fun StepContext.tapEnter() {
        performNativeAction(
            androidKey = AndroidKey.ENTER,
            iosKey = "\n"
        )
    }
}
