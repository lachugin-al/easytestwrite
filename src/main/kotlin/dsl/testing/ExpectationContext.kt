package dsl.testing

/**
 * Контекст проверок в тестовом фреймворке.
 *
 * Используется для описания и выполнения ожиданий (assertions) внутри тестовых шагов.
 * Расширяет базовый функционал [BaseContext], добавляя поддержку платформо-зависимых действий
 * и безопасного выполнения вспомогательных проверок.
 *
 * Все проверки, выполняемые внутри [ExpectationContext], автоматически интегрируются в тестовый отчёт.
 *
 * @see BaseContext
 * @see StepContext
 */
@TestingDslMarker
class ExpectationContext(override val driver: Any) : BaseContext()
