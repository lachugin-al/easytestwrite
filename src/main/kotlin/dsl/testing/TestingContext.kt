package dsl.testing

import com.microsoft.playwright.Page
import io.appium.java_client.AppiumDriver
import io.qameta.allure.Allure.ThrowableRunnable
import io.qameta.allure.Allure.addAttachment
import io.qameta.allure.Allure.step
import org.openqa.selenium.OutputType
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import javax.imageio.stream.ImageOutputStream

/**
 * Контекст выполнения всего теста.
 *
 * [TestingContext] представляет собой верхнеуровневую структуру теста,
 * объединяющую последовательность логических шагов ([StepContext]),
 * с автоматической интеграцией каждого шага в Allure-отчёт.
 *
 * Каждый вызов `"Название шага" { ... }` создаёт новый [StepContext],
 * который в свою очередь может включать множество проверок ([ExpectationContext]).
 *
 * @property driver Объект драйвера (AppiumDriver для мобильных платформ или аналогичный объект для Web).
 *
 * @see BaseContext
 * @see StepContext
 */
@TestingDslMarker
class TestingContext(private val driver: Any) : BaseContext() {

    /** Счётчик шагов внутри теста для отображения в Allure-отчёте. */
    private var currentStep: Int = 1

    /**
     * Создаёт новый тестовый шаг с привязкой к Allure-отчёту.
     *
     * Каждый шаг обрабатывается в рамках нового экземпляра [StepContext].
     *
     * Пример использования:
     * ```
     * "Открыть экран авторизации" {
     *     clickLoginButton()
     *     "Проверка отображения поля ввода" {
     *         checkInputFieldVisible()
     *     }
     * }
     * ```
     *
     * @receiver Название тестового шага.
     * @param stepAction Лямбда с действиями и проверками внутри шага.
     */
    operator fun String.invoke(
        screenshotOnSuccess: Boolean = true,
        screenshotOnFailure: Boolean = true,
        screenshotScale: Double = 0.5,
        screenshotQuality: Int = 100,
        stepAction: StepContext.() -> Unit
    ) {
        val title = "Шаг №$currentStep. $this"

        step(
            title,
            ThrowableRunnable {
                runCatching {
                    StepContext().stepAction()
                }.onSuccess {
                    if (screenshotOnSuccess) {
                        takeScreenshot("$title — успех", screenshotScale, screenshotQuality)
                    }
                }.onFailure { t ->
                    if (screenshotOnFailure) {
                        takeScreenshot("$title — ошибка", screenshotScale, screenshotQuality)
                    }
                    throw t
                }
            })
        currentStep++
    }


    /**
     * Снимает скриншот экрана и прикрепляет к Allure-отчёту.
     * Поддерживает AppiumDriver и Playwright Page.
     */
    fun takeScreenshot(
        name: String,
        scale: Double,
        quality: Int
    ) = optional {
        // получаем байты «сырого» скриншота
        val raw: ByteArray = when (driver) {
            is AppiumDriver<*> -> driver.getScreenshotAs(OutputType.BYTES)
            is Page -> driver.screenshot()
            else -> return@optional
        }

        // масштабируем и/или меняем качество, если нужно
        val processed = if (scale < 1.0 || quality < 100) {
            processImage(raw, scale.coerceIn(0.1, 1.0), quality.coerceIn(1, 100))
        } else raw

        addAttachment(name, "image/png", ByteArrayInputStream(processed), "png")
    }


    /**
     * Масштабирует изображение и задаёт степень сжатия PNG.
     */
    fun processImage(
        bytes: ByteArray,
        scale: Double,
        quality: Int
    ): ByteArray {
        return try {
            val original: BufferedImage = ImageIO.read(ByteArrayInputStream(bytes))
            // масштаб
            val width = (original.width * scale).toInt()
            val height = (original.height * scale).toInt()
            val resized = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).apply {
                createGraphics().also { g ->
                    g.drawImage(original, 0, 0, width, height, null)
                    g.dispose()
                }
            }
            // сжатие PNG
            val writer: ImageWriter = ImageIO.getImageWritersByFormatName("png").next()
            val param: ImageWriteParam = writer.defaultWriteParam.apply {
                if (canWriteCompressed()) {
                    compressionMode = ImageWriteParam.MODE_EXPLICIT

                    val compLevel = (9 * (100 - quality) / 99f)
                    compressionQuality = 1f - compLevel / 9f
                }
            }
            val outStream = ByteArrayOutputStream().also { baos ->
                val ios: ImageOutputStream = ImageIO.createImageOutputStream(baos)
                writer.output = ios
                writer.write(null, IIOImage(resized, null, null), param)
                ios.close()
                writer.dispose()
            }
            outStream.toByteArray()
        } catch (_: Exception) {
            bytes
        }
    }
}
