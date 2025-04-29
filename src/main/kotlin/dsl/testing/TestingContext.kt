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
    operator fun String.invoke(stepAction: StepContext.() -> Unit) {
        step(
            "Шаг №$currentStep. $this",
            ThrowableRunnable {
                StepContext().stepAction()
            }
        )
        currentStep++
    }
}
