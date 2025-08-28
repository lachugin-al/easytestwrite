package reporting.allure

import io.qameta.allure.Allure
import io.qameta.allure.model.Label
import org.junit.jupiter.api.extension.*
import java.util.Optional

/**
 * Расширение JUnit, которое обрабатывает аннотацию [Suite].
 *
 * Это расширение изменяет метку "suite" в отчетах Allure, заменяя стандартное значение
 * (полное имя класса с пакетом) на значение, указанное в аннотации [Suite].
 *
 * Для использования необходимо:
 * 1. Добавить аннотацию [Suite] к тестовому классу с нужным именем Suite
 * 2. Добавить аннотацию [@ExtendWith(SuiteExtension::class)] к тестовому классу
 */
class AllureExtension : BeforeEachCallback {
    /**
     * Метод, вызываемый перед каждым тестом.
     *
     * @param context Контекст выполнения теста, содержащий информацию о тестовом классе и методе.
     */
    override fun beforeEach(context: ExtensionContext) {
        // Получаем значение аннотации Suite из тестового класса
        val suite = context.testClass
            .flatMap { klass -> klass.getAnnotation(Suite::class.java)?.let { Optional.of(it.value) } }

        // Если аннотация Suite присутствует, обновляем метку "suite" в Allure
        suite.ifPresent { value ->
            Allure.getLifecycle().updateTestCase {
                // Удаляем существующую метку "suite"
                it.labels.removeIf { label -> label.name == "suite" }

                // Создаем новую метку "suite" с значением из аннотации
                val label = Label()
                label.name = "suite"
                label.value = value

                // Добавляем новую метку в тестовый кейс
                it.labels.add(label)
            }
        }
    }
}
