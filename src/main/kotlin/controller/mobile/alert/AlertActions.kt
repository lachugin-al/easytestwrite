package controller.mobile.alert

import controller.mobile.core.AppContext
import dsl.testing.StepContext
import utils.DEFAULT_POLLING_INTERVAL
import utils.DEFAULT_TIMEOUT_EXPECTATION

interface AlertActions : AppContext {

    /**
     * DSL-метод для работы с системными алертами из контекста шага.
     *
     * @param accept если true — нажать «Accept» (accept), иначе — «Cancel» (dismiss).
     * @param timeoutExpectation сколько секунд ждать появления алерта.
     * @param pollingInterval Частота опроса элемента в миллисекундах.
     *
     * Пример:
     * ```
     * "Обработка системного алерта" {
     *     alert(accept = true)
     * }
     * ```
     */
    fun StepContext.alert(
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL
    ): AlertHandler = AlertHandler(driver, timeoutExpectation, pollingInterval)
}