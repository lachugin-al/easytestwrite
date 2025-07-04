package utils.allure

/**
 * Используется для пометки тестового класса как Suite (набор тестов).
 * Эта аннотация устанавливает имя Suite в отчетах Allure.
 *
 * @property value Имя Suite, которое будет отображаться в отчетах Allure.
 *                 Если не указано, используется пустая строка.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Suite(val value: String = "")
