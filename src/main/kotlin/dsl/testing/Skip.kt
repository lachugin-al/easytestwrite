package dsl.testing

import java.lang.annotation.Inherited

/**
 * Annotation for skipping tests under certain conditions.
 *
 * This annotation can be applied to test methods to indicate
 * that the test should be skipped on a specific platform or on all platforms.
 * Skipping also applies to methods annotated with @BeforeEach.
 *
 * Usage examples:
 * ```
 * // Skip test on all platforms
 * @Skip
 * @Test
 * fun skippedTest() { ... }
 *
 * // Skip test only on Android
 * @Skip(platform = "android")
 * @Test
 * fun iosOnlyTest() { ... }
 *
 * // Skip test only on iOS
 * @Skip(platform = "ios")
 * @Test
 * fun androidOnlyTest() { ... }
 *
 * // Skip test with a reason
 * @Skip(reason = "Functionality temporarily disabled")
 * @Test
 * fun temporarilyDisabledTest() { ... }
 *
 * // Skip test on a specific platform with a reason
 * @Skip(platform = "android", reason = "Functionality not supported on Android")
 * @Test
 * fun notSupportedOnAndroidTest() { ... }
 * ```
 *
 * @param platform The platform on which the test should be skipped.
 *                 If not specified (empty string), the test will be skipped on all platforms.
 * @param reason The reason for skipping the test. If specified, it will be displayed in the test execution report.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class Skip(val platform: String = "", val reason: String = "")
