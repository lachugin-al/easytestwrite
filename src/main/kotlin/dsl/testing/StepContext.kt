package dsl.testing

import io.qameta.allure.Allure.step
import io.qameta.allure.Allure.ThrowableRunnable

/**
 * Test step execution context.
 *
 * [StepContext] is used to group assertions ([ExpectationContext]) within logical test steps,
 * automatically integrating them into Allure reports with convenient numbering.
 *
 * Each call of `"Step name" { ... }` creates a new isolated [ExpectationContext],
 * within which all assertions and validations are executed.
 *
 * @see BaseContext
 * @see ExpectationContext
 */
@TestingDslMarker
class StepContext(override val driver: Any) : BaseContext() {

    /** Counter of checks within a single step for display in the Allure report. */
    private var currentCheck: Int = 1

    /**
     * Creates a new test step linked to the Allure report.
     *
     * Each step includes one or more checks executed in the context of [ExpectationContext].
     *
     * Example:
     * ```
     * "Step name" {
     *     checkSomething()
     *     validateAnotherThing()
     * }
     * ```
     *
     * @receiver The name of the created step.
     * @param expectationRunnable Lambda containing checks inside the step.
     */
    operator fun String.invoke(
        screenshotOnSuccess: Boolean = false,
        screenshotOnFailure: Boolean = false,
        screenshotScale: Double = 0.5,
        screenshotQuality: Int = 100,
        expectationRunnable: ExpectationContext.() -> Unit
    ) {
        val title = "Check #$currentCheck. $this"
        step(title, ThrowableRunnable {
            val ctx = ExpectationContext(driver)
            runCatching { ctx.expectationRunnable() }
                .onSuccess {
                    if (screenshotOnSuccess) ctx.takeScreenshot("$title — success", screenshotScale, screenshotQuality)
                }
                .onFailure { t ->
                    if (screenshotOnFailure) ctx.takeScreenshot("$title — failure", screenshotScale, screenshotQuality)
                    throw t
                }
        })
        currentCheck++
    }
}
