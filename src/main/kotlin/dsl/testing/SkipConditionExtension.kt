package dsl.testing

import app.App
import app.config.AppConfig
import controller.mobile.MobileTest
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import org.slf4j.LoggerFactory

/**
 * JUnit расширение для обработки аннотации [Skip].
 *
 * Это расширение проверяет наличие аннотации [Skip] на тестовом методе
 * и пропускает тест, если условие в аннотации истинно или если текущая платформа
 * соответствует указанной в аннотации.
 * Расширение также проверяет наличие аннотации [Skip] на тестовом методе
 * при выполнении методов с аннотацией @BeforeEach.
 */
class SkipConditionExtension : ExecutionCondition {
    private val logger = LoggerFactory.getLogger(SkipConditionExtension::class.java)

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        // Получаем текущий метод (тестовый или @BeforeEach)
        val currentMethod = context.testMethod.orElse(null) ?: return ConditionEvaluationResult.enabled("No test method found")

        // Проверяем, является ли текущий метод методом @BeforeEach
        val isBeforeEachMethod = currentMethod.isAnnotationPresent(org.junit.jupiter.api.BeforeEach::class.java)

        // Если это метод @BeforeEach, нам нужно проверить аннотацию Skip на тестовом методе
        if (isBeforeEachMethod) {
            // Получаем тестовый метод из контекста
            val testMethod = findTestMethod(context) ?: return ConditionEvaluationResult.enabled("No test method found for @BeforeEach")

            // Проверяем наличие аннотации Skip на тестовом методе
            val skipAnnotation = testMethod.getAnnotation(Skip::class.java)
            if (skipAnnotation != null && shouldSkip(skipAnnotation)) {
                val reason = if (skipAnnotation.reason.isNotEmpty()) ": ${skipAnnotation.reason}" else ""
                // Закрываем приложение при пропуске теста
                closeAppIfNeeded(context)
                return ConditionEvaluationResult.disabled("Тестовый метод содержит аннотацию @Skip, подходящую под текущие условия$reason")
            }
        } else {
            // Если это тестовый метод, проверяем наличие аннотации Skip на нем
            val skipAnnotation = currentMethod.getAnnotation(Skip::class.java)
            if (skipAnnotation != null && shouldSkip(skipAnnotation)) {
                val reason = if (skipAnnotation.reason.isNotEmpty()) ": ${skipAnnotation.reason}" else ""
                // Закрываем приложение при пропуске теста
                closeAppIfNeeded(context)
                return ConditionEvaluationResult.disabled("Тестовый метод содержит аннотацию @Skip, подходящую под текущие условия$reason")
            }
        }

        return ConditionEvaluationResult.enabled("Аннотация @Skip не найдена или условия не соответствуют")
    }

    /**
     * Проверяет, должен ли тест быть пропущен на основе аннотации Skip.
     *
     * @param skipAnnotation Аннотация Skip.
     * @return true, если тест должен быть пропущен, false в противном случае.
     */
    private fun shouldSkip(skipAnnotation: Skip): Boolean {
        // Проверяем соответствие платформы
        return shouldSkipForPlatform(skipAnnotation.platform)
    }

    /**
     * Определяет, должен ли тест быть пропущен для текущей платформы.
     *
     * @param platform платформа, указанная в аннотации @Skip
     * @return true, если тест должен быть пропущен на текущей платформе, иначе false
     */
    private fun shouldSkipForPlatform(platform: String): Boolean {
        // Если платформа не указана, пропускаем тест на всех платформах
        if (platform.isEmpty()) {
            return true
        }

        // Проверяем, соответствует ли текущая платформа указанной в аннотации
        return when (platform.lowercase()) {
            "ios" -> AppConfig.isiOS()
            "android" -> AppConfig.isAndroid()
            else -> false // Если указана неизвестная платформа, не пропускаем тест
        }
    }

    /**
     * Находит тестовый метод из контекста выполнения.
     * 
     * @param context Контекст выполнения JUnit.
     * @return Тестовый метод или null, если не найден.
     */
    private fun findTestMethod(context: ExtensionContext): java.lang.reflect.Method? {
        // Получаем имя тестового метода из контекста
        val testMethodName = context.testMethod.orElse(null)?.name ?: return null

        // Получаем класс тестового метода
        val testClass = context.testClass.orElse(null) ?: return null

        // Находим тестовый метод по имени
        return testClass.methods.find { 
            it.name == testMethodName && 
            (it.isAnnotationPresent(org.junit.jupiter.api.Test::class.java) || 
             it.isAnnotationPresent(org.junit.jupiter.api.TestTemplate::class.java))
        }
    }

    /**
     * Закрывает приложение, если тест пропускается.
     * 
     * @param context Контекст выполнения JUnit.
     */
    private fun closeAppIfNeeded(context: ExtensionContext) {
        try {
            // Получаем экземпляр тестового класса
            val testInstance = context.testInstance.orElse(null)

            // Проверяем, является ли тестовый класс экземпляром MobileTest
            if (testInstance is MobileTest) {
                logger.info("Тест пропущен из-за аннотации @Skip. Закрываем приложение.")
                // Закрываем приложение через публичный метод
                testInstance.closeApp()
            }
        } catch (e: Exception) {
            logger.error("Ошибка при попытке закрыть приложение: ${e.message}", e)
        }
    }
}
