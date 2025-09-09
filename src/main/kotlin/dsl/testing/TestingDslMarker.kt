package dsl.testing

/**
 * DSL marker to restrict scope visibility within the testing DSL.
 *
 * The [TestingDslMarker] annotation is applied to DSL context classes
 * ([TestingContext], [StepContext], [ExpectationContext]) to prevent
 * incorrect cross-calls between context levels within a single test.
 *
 * Using this marker ensures:
 * - Safe construction of nested test structures.
 * - No conflicts between methods from different DSL levels.
 *
 * @see TestingContext
 * @see StepContext
 * @see ExpectationContext
 */
@DslMarker
annotation class TestingDslMarker
