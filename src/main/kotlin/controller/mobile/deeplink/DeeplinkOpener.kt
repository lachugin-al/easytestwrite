package controller.mobile.deeplink

import app.config.AppConfig
import app.model.Platform
import controller.mobile.element.PageElement
import controller.mobile.core.AppContext
import controller.mobile.interaction.UiElementFinding
import device.EmulatorManager.getSimulatorId
import dsl.testing.StepContext
import org.openqa.selenium.By
import utils.TerminalUtils.runCommand
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

interface DeeplinkOpener : AppContext, UiElementFinding {

    /**
     * Open a deeplink on the mobile device depending on the platform.
     *
     * @param deeplink The deeplink string to be opened.
     *
     * For Android:
     *  - Uses the Mobile Command `mobile:deepLink` via Appium.
     *
     * For iOS:
     *  - Deeplink is opened via simulator using `xcrun simctl openurl`,
     *  - The helper page is served through a local web server.
     *
     * @throws IllegalArgumentException if the platform is not supported.
     */
    fun StepContext.openDeeplink(deeplink: String) {
        when (AppConfig.getPlatform()) {
            // For Android: call the mobile deeplink command via Appium
            Platform.ANDROID -> driver.executeScript(
                "mobile:deepLink",
                mapOf(
                    "url" to deeplink,
                    "package" to AppConfig.getAppPackage()
                )
            )

            // For iOS: first try the standard Appium approach, then (if it fails or bundleId is missing) fall back to simulator
            Platform.IOS -> {
                val bundleId = AppConfig.getBundleId().trim()
                if (bundleId.isNotEmpty()) {
                    try {
                        driver.executeScript(
                            "mobile:deepLink",
                            mapOf(
                                "url" to deeplink,
                                "bundleId" to bundleId
                            )
                        )
                        return
                    } catch (e: Exception) {
                        logger.warn("iOS deepLink via Appium failed, falling back to simulator: ${e.message}")
                    }
                }

                val encodedUrl: String = URLEncoder.encode(deeplink, StandardCharsets.UTF_8)
                val listCommand = listOf(
                    "xcrun",
                    "simctl",
                    "openurl",
                    getSimulatorId(AppConfig.getIosDeviceName()).toString(),
                    app.webServer.getHostingUrl() + "src/main/resources/deeplink.html?url=" + encodedUrl
                )

                // Execute the open URL command through the simulator
                runCommand(listCommand, "Unable to open deeplink")

                // Wait for the "deeplink" element to appear on the simulator screen and click it
                (this@DeeplinkOpener as UiElementFinding).waitForElement(
                    PageElement(
                        android = null,
                        ios = By.id("deeplink")
                    ), timeoutExpectation = 15
                ).click()
            }

            else -> throw IllegalArgumentException("Unsupported platform")
        }
    }
}
