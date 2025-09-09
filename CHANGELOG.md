# Changelog

All notable changes to this project will be documented in this file.

## [0.0.1] - 2025-07-25
### Added
- Initial project setup with package refactoring and configuration
- PageElement improvements and README information
- BundleId support
- Screenshot functionality
- AlertHandler
- Native iOS automatic alert actions
- Native actions
- Flexible locator click functionality
- Multiple optional steps
- Retry logic for failed tests
- Context screenshot disabling
- Native locator strategy
- Console logs in test report
- Event storage and log clearing before each test
- Strategy update for iOS event card lookup
- Video recording functionality
- Parameterized test support
- Separate video recording for Android and iOS
- Timeout expectation check for events
- AnrWatcher utility
- Proxy support and refactoring
- EventPosition parameter for `click`
- SkipConditionExtension for conditional test skipping based on platform
- Closing the app when skipping tests
- Improved Skip annotation with reason support
- Localized test reporting
- EmulatorManager for managing Android and iOS emulators
- Integrated lifecycle management in MobileTest
- Retry logic for Android emulator startup
- Configurable auto-start and shutdown of emulator in AppConfig
- Extended MobileTest lifecycle logic
- New configuration properties
- Improved MobileTest with JSON value matching templates (wildcards, partials)
- Updated `matchJsonElement` logic
- README update with JSON pattern matching docs and Playwright integration
- Extended documentation on parameterized testing with examples
- Improved event/analytics docs with matching examples
- GitHub Actions workflow for automated unit tests
- Documentation for Suite annotation and AllureExtension
- Documentation on working with events and analytics including templates
- Expanded README with full table of contents and navigation

### Improved
- PageElement enhancements
- Flexible click handling
- Multiple optional steps DSL
- Retry strategy for failing tests
- SkipConditionExtension logic with app closing
- MobileTest lifecycle with delayed binding for `app` and `context`
- Emulator startup checks and reliability
- Logging clarity and error details
- Event matching simplification in MobileTest
- Exception handling optimization
- Android version and device name defaults in AppConfig
- MobileTest stability improvements with UI stabilization waits
- Timeout behavior tuning (`timeoutBeforeExpectation`, `timeoutEventExpectation`)
- Scroll/retry logic for event matching
- Improved error reporting with locator details
- Simplified event handling to reduce unnecessary scrolls
- Optimized timeout processing
- Improved test navigation in documentation
- Documentation structure and organization
- Additional framework usage examples
- README with detailed usage guide
- Full table of contents for navigation

### Changed
- Renamed parameter `timeoutExpectation` → `timeoutEventExpectation` in MobileTest

### Fixed
- Improved WebServer shutdown handling and resource cleanup
- Stable termination of locally started processes