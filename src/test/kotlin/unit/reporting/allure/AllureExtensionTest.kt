package unit.reporting.allure

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.qameta.allure.Allure
import io.qameta.allure.AllureLifecycle
import io.qameta.allure.model.Label
import io.qameta.allure.model.TestResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtensionContext
import reporting.allure.AllureExtension
import reporting.allure.Suite
import java.util.Optional
import java.util.function.Consumer
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suite("My Suite Name")
private class AnnotatedTestClass

private class NonAnnotatedTestClass

/**
 * Unit tests for [AllureExtension].
 *
 * Verifies:
 * - Replacing the "suite" label when the test class has the @Suite annotation
 * - Doing nothing when the @Suite annotation is absent
 */
class AllureExtensionTest {

    private fun prepareContext(klass: Class<*>): ExtensionContext {
        val ctx = mockk<ExtensionContext>()
        every { ctx.testClass } returns Optional.of(klass)
        return ctx
    }

    private fun captureUpdateConsumer(lifecycle: AllureLifecycle): CapturingSlot<Consumer<TestResult>> {
        val consumerSlot = slot<Consumer<TestResult>>()
        every { lifecycle.updateTestCase(capture(consumerSlot)) } just runs
        return consumerSlot
    }

    @Test
    fun `beforeEach should replace suite label when @Suite present`() {
        mockkStatic(Allure::class)
        val lifecycle = mockk<AllureLifecycle>(relaxed = true)
        every { Allure.getLifecycle() } returns lifecycle
        val consumerSlot = captureUpdateConsumer(lifecycle)

        val context = prepareContext(AnnotatedTestClass::class.java)

        assertDoesNotThrow { AllureExtension().beforeEach(context) }

        val initial = TestResult()
        initial.labels = mutableListOf(
            Label().apply { name = "suite"; value = "OldSuite" },
            Label().apply { name = "other"; value = "x" }
        )
        assertNotNull(consumerSlot.captured)
        consumerSlot.captured.accept(initial)

        val suiteLabels = initial.labels.filter { it.name == "suite" }
        assertEquals(1, suiteLabels.size)
        assertEquals("My Suite Name", suiteLabels.first().value)

        verify(exactly = 1) { lifecycle.updateTestCase(any()) }
    }

    @Test
    fun `beforeEach should do nothing when no @Suite`() {
        mockkStatic(Allure::class)
        val lifecycle = mockk<AllureLifecycle>(relaxed = true)
        every { Allure.getLifecycle() } returns lifecycle
        val consumerSlot = captureUpdateConsumer(lifecycle)

        val context = prepareContext(NonAnnotatedTestClass::class.java)

        AllureExtension().beforeEach(context)

        verify(exactly = 0) { lifecycle.updateTestCase(any()) }
        assertEquals(false, consumerSlot.isCaptured)
    }
}
