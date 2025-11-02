package controller.mobile.core

import app.App
import io.appium.java_client.AppiumDriver
import io.appium.java_client.MobileElement
import org.slf4j.Logger

/**
 * Unified context: all mixins get the driver from App.
 * We no longer keep separate driver fields in interfaces.
 */
interface AppContext {
    val app: App
    val logger: Logger

    val driver: AppiumDriver<MobileElement>
        get() = app.driver ?: throw IllegalStateException("Driver is not initialized")
}
