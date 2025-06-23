package dsl.testing

import java.lang.annotation.Inherited

/**
 * Аннотация для пропуска тестов при определенных условиях.
 *
 * Эта аннотация может быть применена к тестовым методам для указания,
 * что тест должен быть пропущен на определенной платформе или на всех платформах.
 * Пропуск теста также распространяется на методы с аннотацией @BeforeEach.
 *
 * Примеры использования:
 * ```
 * // Пропустить тест на всех платформах
 * @Skip
 * @Test
 * fun skippedTest() { ... }
 *
 * // Пропустить тест только на Android
 * @Skip(platform = "android")
 * @Test
 * fun iosOnlyTest() { ... }
 *
 * // Пропустить тест только на iOS
 * @Skip(platform = "ios")
 * @Test
 * fun androidOnlyTest() { ... }
 * ```
 *
 * @param platform Платформа, на которой тест должен быть пропущен.
 *                 Если не указана (пустая строка), тест будет пропущен на всех платформах.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class Skip(val platform: String = "")
