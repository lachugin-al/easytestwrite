package dsl.testing

/**
 * Assertion context in the test framework.
 *
 * Used for describing and executing expectations (assertions) inside test steps.
 * Extends the base functionality of [BaseContext], adding support for
 * platform-dependent actions and safe execution of auxiliary checks.
 *
 * All checks performed inside [ExpectationContext] are automatically integrated into the test report.
 *
 * @see BaseContext
 * @see StepContext
 */
@TestingDslMarker
class ExpectationContext(override val driver: Any) : BaseContext()
