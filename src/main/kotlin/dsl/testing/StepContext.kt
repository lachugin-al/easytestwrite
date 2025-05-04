package dsl.testing

import io.qameta.allure.Allure.step
import io.qameta.allure.Allure.ThrowableRunnable

/**
 * Контекст выполнения тестового шага.
 *
 * [StepContext] используется для группировки проверок ([ExpectationContext]) внутри логических шагов теста,
 * автоматически интегрируя их в Allure-отчёты с удобной нумерацией проверок.
 *
 * Каждый вызов `"Название шага" { ... }` создаёт новый изолированный [ExpectationContext],
 * в рамках которого выполняются все проверки и валидации.
 *
 * @see BaseContext
 * @see ExpectationContext
 */
@TestingDslMarker
class StepContext(override val driver: Any) : BaseContext() {

    /** Счётчик проверок внутри одного шага для отображения в Allure-отчёте. */
    private var currentCheck: Int = 1

    /**
     * Создаёт новый тестовый шаг с привязкой к Allure-отчёту.
     *
     * Каждый шаг включает в себя одну или несколько проверок, выполняемых в контексте [ExpectationContext].
     *
     * Пример использования:
     * ```
     * "Название шага" {
     *     checkSomething()
     *     validateAnotherThing()
     * }
     * ```
     *
     * @receiver Название создаваемого шага.
     * @param expectationRunnable Лямбда с проверками внутри шага.
     */
    operator fun String.invoke(
        screenshotOnSuccess: Boolean = true,
        screenshotOnFailure: Boolean = true,
        screenshotScale: Double = 0.5,
        screenshotQuality: Int = 100,
        expectationRunnable: ExpectationContext.() -> Unit
    ) {
        val title = "Проверка №$currentCheck. $this"
        step(title, ThrowableRunnable {
            val ctx = ExpectationContext(driver)
            runCatching { ctx.expectationRunnable() }
                .onSuccess {
                    if (screenshotOnSuccess) ctx.takeScreenshot("$title — успех", screenshotScale, screenshotQuality)
                }
                .onFailure { t ->
                    if (screenshotOnFailure) ctx.takeScreenshot("$title — ошибка", screenshotScale, screenshotQuality)
                    throw t
                }
        })
        currentCheck++
    }
}
