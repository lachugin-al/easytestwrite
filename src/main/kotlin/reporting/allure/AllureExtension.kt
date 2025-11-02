package reporting.allure

import io.qameta.allure.Allure
import io.qameta.allure.model.Label
import org.junit.jupiter.api.extension.*
import java.util.Optional

/**
 * JUnit extension that processes the [Suite] annotation.
 *
 * This extension modifies the "suite" label in Allure reports, replacing the default value
 * (the full class name with package) with the value specified in the [Suite] annotation.
 *
 * Usage:
 * 1. Add the [Suite] annotation to the test class with the desired Suite name.
 * 2. Add the annotation [@ExtendWith(SuiteExtension::class)] to the test class.
 */
class AllureExtension : BeforeEachCallback {
    /**
     * Method called before each test.
     *
     * @param context Test execution context containing information about the test class and method.
     */
    override fun beforeEach(context: ExtensionContext) {
        // Get the Suite annotation value from the test class
        val suite = context.testClass
            .flatMap { klass -> Optional.ofNullable(klass.getAnnotation(Suite::class.java)).map { it.value } }

        // If the Suite annotation is present, update the "suite" label in Allure
        suite.ifPresent { value ->
            Allure.getLifecycle().updateTestCase {
                // Remove existing "suite" label
                it.labels.removeIf { label -> label.name == "suite" }

                // Create a new "suite" label with the value from the annotation
                val label = Label()
                label.name = "suite"
                label.value = value

                // Add the new label to the test case
                it.labels.add(label)
            }
        }
    }
}
