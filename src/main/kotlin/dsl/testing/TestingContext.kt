package dsl.testing

import io.qameta.allure.Allure.ThrowableRunnable
import io.qameta.allure.Allure.step

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
class TestingContext(override val driver: Any) : BaseContext() {

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
        step(title, ThrowableRunnable {
            val ctx = StepContext(driver)
            runCatching { ctx.stepAction() }
                .onSuccess {
                    if (screenshotOnSuccess) ctx.takeScreenshot("$title — успех", screenshotScale, screenshotQuality)
                }
                .onFailure { t ->
                    if (screenshotOnFailure) ctx.takeScreenshot("$title — ошибка", screenshotScale, screenshotQuality)
                    throw t
                }
        })
        currentStep++
    }
}
