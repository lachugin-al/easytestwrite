# Changelog

All notable changes to this project will be documented in this file.

## [0.1.4] - 2025-09-17
- Screenshots: add configurable flags to take screenshots on step success/failure; integrated with AppConfig and step DSL (StepContext/TestingContext); controllable via JVM/Gradle properties: `screenshot.on.success` and `screenshot.on.failure`.
- Tooling: enable Detekt static analysis configuration and formatting plugin for consistent code style.
- WebServer: simplify the debug runner (WebServerMain); streamline start/stop flow and reduce console noise.
- Docs: update README.

## [0.1.3] - 2025-09-09
- Documentation: update README to reflect version 0.1.3.

## [0.1.2] - 2025-09-05
- AppConfig: add screenshot scaling and quality configuration.
- ImageProcessor: add JPEG support with quality/size options.
- Tests: update ImageProcessorTest to specify image format.
- EmulatorManager: ensure Wi‑Fi configuration before tests; utility methods for network setup.
- Dependencies adjusted.

## [0.1.1] - 2025-09-02
- dotenv: support .env files and prioritize environment variables over config.properties.
- Appium runner: integrate Node-based Appium runner with Gradle tasks to setup/start/stop servers; support log forwarding and configurable properties.
- Documentation: extend README with automated Appium setup, logging, and usage.

## [0.0.49] - 2025-08-29
- EmulatorManager: improve iOS simulator responsiveness check via JSON parsing; adjust required tools; streamline error handling.
- MobileTest: add app initialization check in closeApp.
- TerminalUtils: enhance runCommand with timeouts, env and working directory; return structured results; add unit tests.
- EmulatorManager: replace Thread.sleep with waitForCondition; use TerminalUtils.runCommand instead of ProcessBuilder.
- iOS simulator models extracted to SimctlModels; cleanup utility methods; adjust method visibility.
- Tests: add unit tests for SimctlModels, EmulatorManager, AnrWatcher, AllureExtension, AllureLogCapture, ScreenshotProvider, ImageProcessor, and VideoRecorder; add mockk dependency.

## [0.0.48] - 2025-08-27
- MobileTest: replace click extension with pageElementMatchedEvent for reusability and improved scroll retry logic.
- iOS: improve deepLink handling by trying Appium-based attempt before simulator fallback.
- Utils: replace getPrice with getNumber; add NumberParser utility and tests; update README.

## [0.0.47] - 2025-07-25
- CI: add GitHub Actions workflow for unit test automation.
- Tests: re-enable previously commented tests in SkipAnnotationTest.
- Build: adjust project version in build script.
- Docs: update CHANGELOG for 0.0.46 and 0.0.47; fix documentation typos across README.md, WebTest, and MobileTest.

## [0.0.46] - 2025-07-24
- Code quality: integrate Detekt static analysis with default configuration and baseline, then disable plugin in build script.

## [0.0.45] - 2025-07-23
- MobileTest: rename timeoutExpectation parameter to timeoutEventExpectation for clarity.

## [0.0.44] - 2025-07-23
- MobileTest: add timeoutEventExpectation parameter for enhanced flexibility.

## [0.0.43] - 2025-07-22
- MobileTest: simplify event matching logic and streamline exception handling.

## [0.0.42] - 2025-07-16
- MobileTest: late binding for app and context; robust emulator startup checks; enhanced logging for reliability and clarity.

## [0.0.41] - 2025-07-16
- AppConfig: update default Android version and device name.

## [0.0.40] - 2025-07-10
- MobileTest: add template matching for JSON values in eventData (wildcards, partial matches); refine matchJsonElement logic.
- Docs: add documentation and examples for JSON value pattern matching; integrate web testing with Playwright.

## [0.0.39] - 2025-07-04
- Allure: add Suite annotation and AllureExtension for custom suite labels and improved reporting.

## [0.0.38] - 2025-07-01
- MobileTest: introduce UI stabilization waits (WebDriverWait); refine timeoutBeforeExpectation behavior.

## [0.0.37] - 2025-07-01
- MobileTest: enhance retry and scrolling logic for event matching; improve error handling and item selection.

## [0.0.36] - 2025-06-30
- PageElement: add multi-locator support for flexibility across platforms.
- MobileTest: refine scrolling and element matching; improve error messages with last exception and locator details.

## [0.0.35] - 2025-06-29
- MobileTest: simplify event processing by reducing unnecessary scroll attempts; optimize timeout handling.

## [0.0.34] - 2025-06-25
- AppConfig: configurable auto-start and auto-shutdown of emulator; extend MobileTest lifecycle logic; add configuration properties.

## [0.0.33] - 2025-06-24
- WebServer: improve shutdown handling with error logging and safe resource cleanup; unify platform-agnostic closure logic.

## [0.0.32] - 2025-06-24
- EmulatorManager: add retry logic for Android emulator boot.

## [0.0.31] - 2025-06-24
- EmulatorManager: increase boot wait time for stability.

## [0.0.30] - 2025-06-24
- EmulatorManager: add Android emulator boot wait logic.
- Utils: introduce TerminalUtils.runCommand helper.

## [0.0.29] - 2025-06-23
- EmulatorManager: add management for Android and iOS emulators; integrate lifecycle management into MobileTest.

## [0.0.28] - 2025-06-23
- SkipConditionExtension: add reason support and localized test reporting.
- MobileTest: expose closeApp; close app automatically when skipping tests.

## [0.0.27] - 2025-06-23
- SkipConditionExtension: conditional test skipping based on platform.

## [0.0.26] - 2025-06-19
- click: add eventPosition parameter.

## [0.0.25] - 2025-06-04
- Proxy: add proxy support and related refactoring.

## [0.0.24] - 2025-05-28
- Device: add AnrWatcher utility.

## [0.0.23] - 2025-05-25
- MobileTest: add default timeout check for event expectation.

## [0.0.22] - 2025-05-17
- Video: record Android and iOS videos separately.

## [0.0.21] - 2025-05-16
- Tests: add parameterized test support.

## [0.0.20] - 2025-05-15
- Video: add video recording for tests.

## [0.0.19] - 2025-05-14
- iOS: update strategy for event card lookup.

## [0.0.18] - 2025-05-13
- Reporting: clear console logs and event storage before each test.

## [0.0.17] - 2025-05-12
- Reporting: add console logs to test report.

## [0.0.16] - 2025-05-10
- Locators: add native locator strategy.

## [0.0.15] - 2025-05-10
- Reporting: disable context screenshot for expectations.

## [0.0.14] - 2025-05-09
- Stability: add retry logic for failed tests.

## [0.0.13] - 2025-05-09
- DSL: add multiple optional steps.

## [0.0.12] - 2025-05-07
- Locators: update flexible locator click.

## [0.0.11] - 2025-05-07
- Locators: add flexible locator click.

## [0.0.10] - 2025-05-06
- Elements: add contains name/label value lookup.

## [0.0.9] - 2025-05-06
- Actions: add native actions.

## [0.0.8] - 2025-05-05
- iOS: automatic alert actions.

## [0.0.7] - 2025-05-04
- Screenshot functionality.
- Add AlertHandler component.

## [0.0.6] - 2025-05-02
- App: add bundleId support.

## [0.0.5] - 2025-05-02
- App: add bundleId support.

## [0.0.4] - 2025-05-01
- PageElement improvements and README updates.

## [0.0.3] - 2025-05-01
- PageElement improvements and README updates.

## [0.0.2] - 2025-05-01
- PageElement improvements and README updates.

## [0.0.1] - 2025-04-30
- Initial project setup: package refactoring and configuration.