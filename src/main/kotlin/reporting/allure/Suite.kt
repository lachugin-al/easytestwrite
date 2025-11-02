package reporting.allure

/**
 * Used to mark a test class as a Suite (a set of tests).
 * This annotation sets the Suite name in Allure reports.
 *
 * @property value Suite name to be displayed in Allure reports.
 *                 If not specified, an empty string is used.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Suite(val value: String = "")
