package unit.reporting.allure

import org.junit.jupiter.api.Test
import reporting.allure.Suite
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import java.lang.annotation.ElementType
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suite
private class DefaultSuiteClass

@Suite("Explicit")
private class ExplicitSuiteClass

class SuiteAnnotationTest {

    @Test
    fun `annotation has runtime retention and class target`() {
        val retention = Suite::class.java.getAnnotation(Retention::class.java)
        val target = Suite::class.java.getAnnotation(Target::class.java)
        assertNotNull(retention)
        assertEquals(RetentionPolicy.RUNTIME, retention.value)
        assertNotNull(target)
        assert(target.value.contains(ElementType.TYPE))
    }

    @Test
    fun `default value is empty string`() {
        val ann = DefaultSuiteClass::class.java.getAnnotation(Suite::class.java)
        assertNotNull(ann)
        assertEquals("", ann.value)
    }

    @Test
    fun `explicit value is preserved`() {
        val ann = ExplicitSuiteClass::class.java.getAnnotation(Suite::class.java)
        assertNotNull(ann)
        assertEquals("Explicit", ann.value)
    }
}
