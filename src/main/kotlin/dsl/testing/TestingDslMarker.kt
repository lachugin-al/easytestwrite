package dsl.testing

/**
 * DSL-маркер для ограничения областей видимости внутри тестового DSL.
 *
 * Аннотация [TestingDslMarker] применяется к классам DSL-контекстов ([TestingContext], [StepContext], [ExpectationContext]),
 * чтобы предотвратить некорректное пересечение вызовов между уровнями контекста в рамках одного теста.
 *
 * Использование маркера обеспечивает:
 * - Безопасность построения вложенных тестовых структур.
 * - Отсутствие конфликтов между методами разных уровней DSL.
 *
 * @see TestingContext
 * @see StepContext
 * @see ExpectationContext
 */
@DslMarker
annotation class TestingDslMarker
