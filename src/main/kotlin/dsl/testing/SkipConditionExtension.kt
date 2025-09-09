package dsl.testing

import app.App
import app.config.AppConfig
import controller.mobile.base.MobileTest
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import org.slf4j.LoggerFactory

/**
 * JUnit extension for handling the [Skip] annotation.
 *
 * This extension checks for the presence of the [Skip] annotation on a test method
 * and skips the test if the annotation's condition is met or if the current platform
 * matches the one specified in the annotation.
 *
 * The extension also checks for the [Skip] annotation on the corresponding test method
 * when executing methods annotated with @BeforeEach.
 */
class SkipConditionExtension : ExecutionCondition {
    private val logger = LoggerFactory.getLogger(SkipConditionExtension::class.java)

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        // Get the current method (test or @BeforeEach)
        val currentMethod = context.testMethod.orElse(null) ?: return ConditionEvaluationResult.enabled("No test method found")

        // Check if the current method is a @BeforeEach method
        val isBeforeEachMethod = currentMethod.isAnnotationPresent(org.junit.jupiter.api.BeforeEach::class.java)

        // If it's a @BeforeEach method, we need to check the Skip annotation on the test method
        if (isBeforeEachMethod) {
            val testMethod = findTestMethod(context) ?: return ConditionEvaluationResult.enabled("No test method found for @BeforeEach")

            val skipAnnotation = testMethod.getAnnotation(Skip::class.java)
            if (skipAnnotation != null && shouldSkip(skipAnnotation)) {
                val reason = if (skipAnnotation.reason.isNotEmpty()) ": ${skipAnnotation.reason}" else ""
                closeAppIfNeeded(context)
                return ConditionEvaluationResult.disabled("Test method contains @Skip annotation matching current conditions$reason")
            }
        } else {
            // If it's a test method, check the Skip annotation directly
            val skipAnnotation = currentMethod.getAnnotation(Skip::class.java)
            if (skipAnnotation != null && shouldSkip(skipAnnotation)) {
                val reason = if (skipAnnotation.reason.isNotEmpty()) ": ${skipAnnotation.reason}" else ""
                closeAppIfNeeded(context)
                return ConditionEvaluationResult.disabled("Test method contains @Skip annotation matching current conditions$reason")
            }
        }

        return ConditionEvaluationResult.enabled("@Skip annotation not found or conditions not met")
    }

    /**
     * Checks whether the test should be skipped based on the Skip annotation.
     *
     * @param skipAnnotation The Skip annotation.
     * @return true if the test should be skipped, false otherwise.
     */
    private fun shouldSkip(skipAnnotation: Skip): Boolean {
        return shouldSkipForPlatform(skipAnnotation.platform)
    }

    /**
     * Determines whether the test should be skipped for the current platform.
     *
     * @param platform The platform specified in the @Skip annotation.
     * @return true if the test should be skipped on the current platform, false otherwise.
     */
    private fun shouldSkipForPlatform(platform: String): Boolean {
        if (platform.isEmpty()) {
            return true
        }

        return when (platform.lowercase()) {
            "ios" -> AppConfig.isiOS()
            "android" -> AppConfig.isAndroid()
            else -> false // Unknown platform, do not skip
        }
    }

    /**
     * Finds the test method from the execution context.
     *
     * @param context The JUnit execution context.
     * @return The test method or null if not found.
     */
    private fun findTestMethod(context: ExtensionContext): java.lang.reflect.Method? {
        val testMethodName = context.testMethod.orElse(null)?.name ?: return null
        val testClass = context.testClass.orElse(null) ?: return null

        return testClass.methods.find {
            it.name == testMethodName &&
                    (it.isAnnotationPresent(org.junit.jupiter.api.Test::class.java) ||
                            it.isAnnotationPresent(org.junit.jupiter.api.TestTemplate::class.java))
        }
    }

    /**
     * Closes the application if the test is skipped.
     *
     * @param context The JUnit execution context.
     */
    private fun closeAppIfNeeded(context: ExtensionContext) {
        try {
            val testInstance = context.testInstance.orElse(null)

            if (testInstance is MobileTest) {
                logger.info("Test skipped due to @Skip annotation. Closing the app.")
                testInstance.closeApp()
            }
        } catch (e: Exception) {
            logger.error("Error while attempting to close the app: ${e.message}", e)
        }
    }
}
