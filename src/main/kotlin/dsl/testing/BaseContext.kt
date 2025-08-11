package dsl.testing

import app.config.AppConfig
import app.model.Platform
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
     * Выполняет несколько блоков кода в безопасном режиме только для iOS платформы.
     *
     * Каждый блок выполняется независимо от результата выполнения предыдущих блоков.
     * Если в каком-либо блоке произойдёт ошибка, она будет проглочена, и выполнение продолжится
     * со следующего блока. Блоки будут выполнены только если текущая платформа - iOS.
     *
     * Пример использования:
     * ```
     * optionalIos(
     *     { "Шаг 1" { действие1() } },
     *     { "Шаг 2" { действие2() } }
     * )
     * ```
     *
     * @param actions Набор лямбд с необязательной логикой выполнения.
     */
    @Suppress("SwallowedException")
    fun optionalIos(vararg actions: () -> Unit) {
        if (AppConfig.getPlatform() != Platform.IOS) return

        actions.forEach { action ->
            try {
                action.invoke()
            } catch (e: Exception) {
                // Ошибки подавляются, чтобы продолжить выполнение следующих шагов
            }
        }
    }

    /**
     * Выполняет несколько блоков кода в безопасном режиме только для Android платформы.
     *
     * Каждый блок выполняется независимо от результата выполнения предыдущих блоков.
     * Если в каком-либо блоке произойдёт ошибка, она будет проглочена, и выполнение продолжится
     * со следующего блока. Блоки будут выполнены только если текущая платформа - Android.
     *
     * Пример использования:
     * ```
     * optionalAndroid(
     *     { "Шаг 1" { действие1() } },
     *     { "Шаг 2" { действие2() } }
     * )
     * ```
     *
     * @param actions Набор лямбд с необязательной логикой выполнения.
     */
    @Suppress("SwallowedException")
    fun optionalAndroid(vararg actions: () -> Unit) {
        if (AppConfig.getPlatform() != Platform.ANDROID) return

        actions.forEach { action ->
            try {
                action.invoke()
            } catch (e: Exception) {
                // Ошибки подавляются, чтобы продолжить выполнение следующих шагов
            }
        }
    }

    /**
     * Выполняет несколько блоков кода в безопасном режиме.
     *
     * Каждый блок выполняется независимо от результата выполнения предыдущих блоков.
     * Если в каком-либо блоке произойдёт ошибка, она будет проглочена, и выполнение продолжится
     * со следующего блока.
     *
     * Пример использования:
     * ```
     * optional(
     *     { step1() },
     *     { step2() },
     *     { step3() }
     * )
     * ```
     *
     * @param actions Набор лямбд с необязательной логикой выполнения.
     */
    @Suppress("SwallowedException")
    fun optional(vararg actions: () -> Unit) {
        actions.forEach { action ->
            try {
                action.invoke()
            } catch (e: Exception) {
                // Ошибки подавляются, чтобы продолжить выполнение следующих шагов
            }
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
    ) {
        optional(
            {
                val provider: ScreenshotProvider = when (driver) {
                    is AppiumDriver<*> -> AppiumScreenshotProvider(driver as AppiumDriver<*>)
                    else -> null
                } ?: return@optional

                val raw = provider.getRawScreenshot()
                val processed = if (scale < 1.0 || quality < 100)
                    ImageProcessor.processImage(raw, scale.coerceIn(0.1, 1.0), quality.coerceIn(1, 100))
                else raw

                addAttachment(name, "image/png", ByteArrayInputStream(processed), "png")
            }
        )
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
