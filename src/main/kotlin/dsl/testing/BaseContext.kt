package dsl.testing

import app.config.AppConfig
import app.model.Platform
import com.microsoft.playwright.Page
import io.appium.java_client.AppiumDriver
import io.qameta.allure.Allure.addAttachment
import utils.screenshot.AppiumScreenshotProvider
import utils.screenshot.ImageProcessor
import utils.screenshot.PlaywrightScreenshotProvider
import utils.screenshot.ScreenshotProvider
import java.io.ByteArrayInputStream

/**
 * Базовый класс всех контекстов тестирования.
 *
 * Предоставляет вспомогательные методы для выполнения кода в зависимости от платформы,
 * а также для безопасного выполнения опциональных действий без прерывания теста.
 *
 * @see TestingDslMarker
 */
@TestingDslMarker
abstract class BaseContext {

    /**
     * Драйвер, предоставляемый потомками для скриншотов.
     */
    protected abstract val driver: Any

    /**
     * Выполняет переданный блок кода только в случае, если целевая платформа — iOS.
     *
     * Блок [action] будет проигнорирован для всех остальных платформ.
     *
     * @param action Лямбда, содержащая действия для выполнения на iOS.
     */
    fun onlyIos(action: () -> Unit) = action.invokeIfRequiredPlatform(Platform.IOS)

    /**
     * Выполняет переданный блок кода только в случае, если целевая платформа — Android.
     *
     * Блок [action] будет проигнорирован для всех остальных платформ.
     *
     * @param action Лямбда, содержащая действия для выполнения на Android.
     */
    fun onlyAndroid(action: () -> Unit) = action.invokeIfRequiredPlatform(Platform.ANDROID)

    /**
     * Выполняет переданный блок кода в безопасном режиме.
     *
     * Если внутри [action] произойдёт ошибка — тест продолжит выполнение без прерывания.
     * Исключение будет проглочено без дополнительной обработки.
     *
     * Используется для необязательных проверок, скриншотов или вспомогательных операций.
     *
     * @param action Лямбда с необязательной логикой выполнения.
     */
    @Suppress("SwallowedException")
    fun optional(action: () -> Unit) {
        try {
            action.invoke()
        } catch (e: Exception) {
            // Ошибки подавляются, чтобы тест продолжал выполнение
        }
    }

    /**
     * Снимает скриншот и прикрепляет его к Allure-отчёту.
     * Выполняется в {@link #optional}.
     *
     * @param name      Название вложения в отчёте.
     * @param scale     Масштаб изображения (0.1–1.0).
     * @param quality   Качество PNG (1–100).
     */
    fun takeScreenshot(
        name: String,
        scale: Double = 0.5,
        quality: Int = 100
    ) = optional {
        val provider: ScreenshotProvider = when (driver) {
            is AppiumDriver<*> -> AppiumScreenshotProvider(driver as AppiumDriver<*>)
            is Page -> PlaywrightScreenshotProvider(driver as Page)
            else -> return@optional
        }

        val raw = provider.getRawScreenshot()
        val processed = if (scale < 1.0 || quality < 100)
            ImageProcessor.processImage(raw, scale.coerceIn(0.1, 1.0), quality.coerceIn(1, 100))
        else raw

        addAttachment(name, "image/png", ByteArrayInputStream(processed), "png")
    }

    /**
     * Вспомогательная функция для условного выполнения блока кода в зависимости от платформы.
     *
     * @param platform Платформа, для которой необходимо выполнить код.
     */
    private fun (() -> Unit).invokeIfRequiredPlatform(platform: Platform) {
        if (AppConfig.getPlatform() == platform) invoke()
    }
}
