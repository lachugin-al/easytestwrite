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
     * Универсальный метод для отправки нативных команд (например, нажатий клавиш) на мобильное устройство.
     *
     * Используйте этот метод для передачи платформенно-зависимых команд в рамках тестовых сценариев.
     *
     * @param androidKey Клавиша Android (например, AndroidKey.BACK, AndroidKey.ENTER).
     *                   Передаётся для платформы Android, для остальных платформ параметр игнорируется.
     * @param iosKey Строка-код клавиши для iOS (например, "\n" для Enter).
     *               Передаётся для iOS, для остальных платформ параметр игнорируется.
     *
     * @throws IllegalArgumentException если не передан необходимый параметр для указанной платформы.
     * @throws UnsupportedOperationException если метод вызван для неподдерживаемой платформы (например, Web).
     */
    fun StepContext.performNativeAction(
        androidKey: AndroidKey? = null,
        iosKey: String? = null
    ) {
        when (AppConfig.getPlatform()) {
            Platform.ANDROID -> {
                val androidDriver = driver as? AndroidDriver ?: error("Driver не AndroidDriver")
                if (androidKey != null) {
                    androidDriver.pressKey(KeyEvent(androidKey))
                } else {
                    throw IllegalArgumentException("Необходимо передать параметр androidKey для Android-платформы")
                }
            }

            Platform.IOS -> {
                val iosDriver = driver as? IOSDriver ?: error("Driver не IOSDriver")
                if (iosKey != null) {
                    iosDriver.keyboard.pressKey(iosKey)
                } else {
                    throw IllegalArgumentException("Необходимо передать параметр iosKey для iOS-платформы")
                }
            }
        }
    }

    /**
     * Выполняет нативное нажатие клавиши Enter (или Return) на мобильном устройстве.
     *
     * Использует платформо-зависимые ключи для Android и iOS:
     * - Android: AndroidKey.ENTER
     * - iOS: "\n"
     *
     * @throws IllegalArgumentException если платформа не поддерживается или драйвер не инициализирован.
     */
    fun StepContext.tapEnter() {
        performNativeAction(
            androidKey = AndroidKey.ENTER,
            iosKey = "\n"
        )
    }
}