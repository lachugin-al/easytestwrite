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
     * Открыть диплинк на мобильном устройстве в зависимости от платформы.
     *
     * @param deeplink Строка диплинка, который необходимо открыть.
     *
     * Для Android:
     *  - Используется Mobile Command `mobile:deepLink` через Appium.
     *
     * Для iOS:
     *  - Открытие диплинка происходит через симулятор с помощью `xcrun simctl openurl`,
     *  - Вспомогательная страница запускается через локальный web-сервер.
     *
     * @throws IllegalArgumentException если платформа не поддерживается.
     */
    fun StepContext.openDeeplink(deeplink: String) {
        when (AppConfig.getPlatform()) {
            // Для Android вызываем команду мобильного диплинка через Appium
            Platform.ANDROID -> driver.executeScript(
                "mobile:deepLink",
                mapOf(
                    "url" to deeplink,
                    "package" to AppConfig.getAppPackage()
                )
            )

            // Для iOS: сначала пробуем стандартный способ через Appium, затем (при ошибке или отсутствии bundleId) эмулируем через симулятор
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

                // Выполняем команду открытия URL через симулятор
                runCommand(listCommand, "Нет возможность открыть deeplink")

                // Ждём появления элемента "deeplink" на экране симулятора и нажимаем по нему
                (this@DeeplinkOpener as UiElementFinding).waitForElement(
                    PageElement(
                        android = null,
                        ios = By.id("deeplink")
                    ), timeoutExpectation = 15
                ).click()
            }

            else -> throw IllegalArgumentException("Неподдерживаемая платформа")
        }
    }
}