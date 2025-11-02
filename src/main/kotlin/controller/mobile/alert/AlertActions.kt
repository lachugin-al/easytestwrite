package controller.mobile.alert

import controller.mobile.core.AppContext
import dsl.testing.StepContext
import utils.DEFAULT_POLLING_INTERVAL
import utils.DEFAULT_TIMEOUT_EXPECTATION

interface AlertActions : AppContext {

    /**
     * DSL method for working with system alerts from the step context.
     *
     * @param accept if true — press "Accept", otherwise — "Cancel" (dismiss).
     * @param timeoutExpectation how many seconds to wait for the alert to appear.
     * @param pollingInterval frequency of element polling in milliseconds.
     *
     * Example:
     * ```
     * "Handling system alert" {
     *     alert(accept = true)
     * }
     * ```
     */
    fun StepContext.alert(
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL
    ): AlertHandler = AlertHandler(driver, timeoutExpectation, pollingInterval)
}
