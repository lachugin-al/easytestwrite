package controller.mobile.core

import app.App
import io.appium.java_client.AppiumDriver
import io.appium.java_client.MobileElement
import org.slf4j.Logger

/**
 * Единый контекст: все миксины получают driver из App.
 * Никаких собственных полей driver в интерфейсах больше не держим.
 */
interface AppContext {
    val app: App
    val logger: Logger

    val driver: AppiumDriver<MobileElement>
        get() = app.driver ?: throw IllegalStateException("Драйвер не инициализирован")
}