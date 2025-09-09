package dsl.testing

import io.qameta.allure.Allure.ThrowableRunnable
import io.qameta.allure.Allure.step

/**
 * Execution context of the entire test.
 *
 * [TestingContext] is a top-level test structure that combines a sequence of logical
 * steps ([StepContext]) with automatic integration of each step into the Allure report.
 *
 * Each call `"Step name" { ... }` creates a new [StepContext], which in turn can include
 * multiple checks ([ExpectationContext]).
 *
 * @property driver Driver object (AppiumDriver for mobile platforms or a similar object for Web).
 *
 * @see BaseContext
 * @see StepContext
 */
@TestingDslMarker
class TestingContext(override val driver: Any) : BaseContext() {

    /** Step counter within the test for displaying in the Allure report. */
    private var currentStep: Int = 1

    /**
     * Creates a new test step with binding to the Allure report.
     *
     * Each step is handled within a new instance of [StepContext].
     *
     * Usage example:
     * ```
     * "Open Authorization screen" {
     *     clickLoginButton()
     *     "Verify input field is visible" {
     *         checkInputFieldVisible()
     *     }
     * }
     * ```
     *
     * @receiver Step title.
     * @param stepAction Lambda with actions and assertions inside the step.
     */
    operator fun String.invoke(
        screenshotOnSuccess: Boolean = true,
        screenshotOnFailure: Boolean = true,
        screenshotScale: Double = 0.5,
        screenshotQuality: Int = 100,
        stepAction: StepContext.() -> Unit
    ) {
        val title = "Step #$currentStep. $this"
        step(title, ThrowableRunnable {
            val ctx = StepContext(driver)
            runCatching { ctx.stepAction() }
                .onSuccess {
                    if (screenshotOnSuccess) ctx.takeScreenshot("$title — success", screenshotScale, screenshotQuality)
                }
                .onFailure { t ->
                    if (screenshotOnFailure) ctx.takeScreenshot("$title — error", screenshotScale, screenshotQuality)
                    throw t
                }
        })
        currentStep++
    }
}
