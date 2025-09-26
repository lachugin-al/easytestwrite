# EasyTestWrite – a framework for mobile application testing

[![version](https://img.shields.io/badge/version-0.1.3-blue.svg)](CHANGELOG.md)
[![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)](LICENSE.txt)

## Table of Contents
- [Overview](#overview)
- [Getting Started](#getting-started)
    - [Environment Setup](#environment-setup-before-starting)
    - [Project Structure](#project-structure)
- [Running Tests](#running-tests)
    - [Local Run](#local-run-of-e2e-tests)
    - [Embedded Appium server management (Kotlin)](#embedded-appium-server-management-kotlin)
    - [Running with Tags](#running-tests-with-tags)
    - [Running with Emulator Management](#running-tests-with-automatic-emulator-management)
- [Writing Tests](#writing-tests)
    - [UI Test Structure](#ui-test-structure)
    - [Types of Tests](#types-of-tests)
    - [Special Blocks](#special-blocks-for-test-organization)
    - [Test Lifecycle](#test-lifecycle)
    - [Skipping Tests](#skipping-tests-with-the-skip-annotation)
- [PageObject Model](#pageobject-structure-description)
    - [Element Locators](#advanced-locator-capabilities)
- [Framework API](#method-descriptions-each-function-has-detailed-documentation)
    - [Data Retrieval Methods](#data-retrieval-methods)
    - [Interaction Methods](#stepcontext)
    - [Verification Methods](#expectationcontext)
    - [Helper Methods](#helper-methods)
    - [Method Parameters](#description-of-possible-parameters-for-functions)
- [Working with Events and Analytics](#working-with-events-and-analytics)
    - [Architecture](#event-processing-architecture)
    - [Event Verification](#event-verification)
    - [Matching Templates](#templates-for-matching-values-in-eventdata)
    - [Interacting with Elements](#interacting-with-elements-based-on-events)
    - [Event Debugging](#event-debugging)
- [Allure Integration](#allure)
    - [Features](#key-features-of-allure-integration)
    - [Suite Annotation](#using-the-suite-annotation)
    - [Report Generation](#report-generation)
- [Advanced Features](#advanced-framework-features)
    - [Emulator Management](#automatic-management-of-emulators-and-simulators)
    - [Parameterized Testing](#parameterized-testing)
    - [Video Recording](#video-recording-settings)
- [API Documentation](#api-documentation-dokka)
- [Conclusion](#conclusion)

## Overview

**Usage:**
- for automating end-to-end testing of mobile app user flows
- for automating analytics testing
- for creating parameterized tests with various data sources

## Getting Started

### Environment Setup (Before You Start):
1. Install `JDK 21` - [guide](https://www.oracle.com/java/technologies/downloads/#java21)
2. Open the `Terminal`
3. Set environment variables: `ANDROID_SDK_ROOT`, `ANDROID_HOME`, `JAVA_HOME`
4. Install `Appium` - [guide](https://github.com/appium/appium)
5. Install native drivers for Android and iOS:
    1. Android - [guide](https://github.com/appium/appium-uiautomator2-driver)
    2. iOS - [guide](https://github.com/appium/appium-xcuitest-driver)
6. Install `FFmpeg` for video recording:
    - macOS: `brew install ffmpeg`
    - Linux: `sudo apt install ffmpeg`
    - Windows: `choco install ffmpeg` or `winget install ffmpeg`
7. Open the project in `Android Studio`
8. Go to `Device Manager` and click `+ Create Virtual Device`, then choose `Pixel 2` and `API 34`
9. Install `Xcode` (macOS only)
10. Open `Xcode`, go to `Open Developer Tool -> Simulator`, then create an `iPhone 16 Plus`

### Project Structure

All source code is located in the `src/main/` folder and split by packages:
* `app` - classes for connecting and launching the app under test
    * `config` - configuration classes
    * `driver` - Android and iOS driver classes
    * `model` - data models
* `controller` - controller classes
    * `mobile` - controllers for mobile tests, classes for working with UI elements, handlers for specific interactions (alerts, etc.)
* `device` - 
* `dsl` - DSL classes for writing tests
* `events` - classes for working with events and analytics
* `proxy` - proxy server classes for intercepting and analyzing network traffic
* `reporting` - reporting components and artifacts
    * `artifacts` - test artifacts
        * `screenshot` - screenshot capture utilities
        * `video` - classes for test video recording
* `utils` - utility classes
    * `allure` - Allure report extensions

All tests are located in the `src/test/` folder and split by packages:
* `mobile` - test packages
    * `uipages` - Page Object classes for the app layout
    * `uitests` - UI test classes
* `resources` - test resources (configuration, test data)

## Running Tests

### Local Run of E2E Tests

#### Preparation for Launch
1. Optional: you can start the Appium server manually (otherwise the framework will start a local Appium automatically when tests begin). In the terminal, you can run:
   ```bash
   appium server --allow-cors
   ```

#### Local Run of E2E Tests on Android and iOS
1.	In the `test/resources` folder, create a configuration file `config.properties`
2.	Fill in the configuration settings (otherwise default values will be applied)
```config.properties
    # URL of the Appium server to connect to mobile devices
    appium.url=http://localhost:4723/
    # Platform for testing (ANDROID or iOS)
    platform=ANDROID
    # Android OS version for testing
    android.version=16
    # iOS version for testing
    ios.version=18.5
    # Android device name for emulation
    android.device.name=Pixel_XL
    # iOS device name for emulation
    ios.device.name=iPhone 16 Plus
    # Android app file name
    android.app.name=android.apk
    # iOS app file name
    ios.app.name=ios.app
    # Main activity of the Android app
    app.activity=MainActivity
    # Android app package identifier
    app.package=com.ru.dev
    # Automatically accept alerts on iOS
    ios.auto_accept_alerts=true
    # Automatically dismiss alerts on iOS
    ios.auto_dismiss_alerts=false
    # Run Android emulator in headless mode
    android.headless.mode=true
    
    # Enable/disable video recording for Android tests
    android.video.recording.enabled=false
    # Enable/disable video recording for iOS tests
    ios.video.recording.enabled=false
    # Test video resolution
    video.recording.size=640x360
    # Test video quality (from 1 to 100)
    video.recording.quality=20
    # Test video bitrate in bits per second
    video.recording.bitrate=50000
    # Directory for saving test videos
    video.recording.output.dir=build/videos
    
    # Emulator/simulator auto start and shutdown settings
    # Automatically start emulator/simulator before tests
    emulator.auto.start=true
    # Automatically shut down emulator/simulator after tests
    emulator.auto.shutdown=true
```
3. Select the folder with tests or specific tests to run, open the context menu, and click `Run ...`

### Embedded Appium server management (Kotlin)
The framework now manages the Appium server directly from Kotlin code via `AppiumServerManager` — no separate Node.js subproject or Gradle tasks are required.

How it works when you run tests (`./gradlew test`):
- At test startup (`@BeforeAll` in `MobileTest`), the framework reads `appium.url` from config/ENV/JVM props.
- If an Appium server at that URL responds as healthy (`/status`), the framework adopts it and starts background health monitoring.
- If no server is available, the framework starts a local Appium process using the `appium` CLI with `--address <host> --port <port>`, waits for readiness (up to ~30s), and continues.
- During the run, a monitor checks health every ~3s and will automatically attempt a restart if the process becomes unhealthy (after a small failure threshold).
- At test shutdown (`@AfterAll`), the framework stops the Appium process only if it was started by the framework; if you connected to an already running server, it is left as-is.

Prerequisites:
- Appium CLI installed and available on PATH (global install):
  ```bash
  npm i -g appium
  ```
- Required platform drivers installed for your use case (examples):
  ```bash
  appium driver install uiautomator2
  appium driver install xcuitest
  ```
- FFmpeg must be installed if video recording is enabled (see Getting Started).

Configuration and usage:
- Base URL: `appium.url` (default: `http://localhost:4723/`). You can set it via:
  - Gradle: `./gradlew test -Pappium.url=http://localhost:4723/`
  - JVM: `-Dappium.url=http://localhost:4723/`
  - .env: `APPIUM_URL=http://localhost:4723/`
  - `src/test/resources/config.properties`.
- Local run (default):
  ```bash
  ./gradlew test -Pplatform=ANDROID
  ```
- Remote Appium:
  ```bash
  ./gradlew test -Pappium.url=http://remote-host:4723/ -Pplatform=ANDROID
  ```
  Note: the framework does not start remote servers. It will attempt to start a local Appium bound to the specified host/port if the URL is unhealthy. For remote usage, ensure the remote Appium is already running and reachable.

Notes and diagnostics:
- Start timeout: ~30s; health poll interval: ~3s; automatic restart on repeated health check failures.
- Logs from the embedded Appium process are forwarded to the test logs with the `[appium]` prefix at DEBUG level.
- You can still set `appium.url` in `config.properties`; CLI `-Pappium.url` or JVM `-Dappium.url` will override it for the current run.

### Configuration sources and precedence
Configuration values can be provided from multiple sources. Priority (highest first):
1. JVM system properties (-Dkey=value) and Gradle -P parameters (forwarded as system properties by the build).
2. .env file variables or environment variables (both key and UPPER_SNAKE_CASE forms are supported).
3. src/test/resources/config.properties.
4. Built-in defaults.

Examples:
- `-Pappium.url` or `-Dappium.url` overrides `APPIUM_URL`/.env and `config.properties`.
- In .env, you can use `APPIUM_URL` or `appium.url`. Dotted names are also resolved in uppercase underscore form: `appium.url` -> `APPIUM_URL`.

(See src/main/kotlin/app/config/AppConfig.kt for implementation details.)

### Running Tests with Tags
To run tests with specific tags, use the `-Ptag` parameter:

```bash
./gradlew test -Ptag=Smoke,Regression
```

This will run only the tests annotated with `@Tag("Smoke")` or `@Tag("Regression")`.

### Running Tests with Automatic Emulator Management
To automatically start and stop emulators/simulators:

```bash
./gradlew test -Pemulator.auto.start=true -Pemulator.auto.shutdown=true
```

## Writing Tests

### UI Test Structure
Tests must be located in the `test/kotlin/uitests/` folder and use the Page Object model with element mappings.
Organize tests into packages depending on the product area the test belongs to.
Each test must be annotated with `@Test`.

### Test Types
The framework supports several types of tests:

1. **Mobile tests** — inherit from the `MobileTest` class and are used to test mobile applications on Android and iOS
2. **Parameterized tests** — use the `@ParameterizedTest` annotation with various data sources
3. **Tagged tests** — are marked with the `@Tag` annotation for selective execution

### Skipping Tests with the `@Skip` Annotation
The framework provides the `@Skip` annotation for conditionally skipping tests:

```kotlin
// Unconditionally skip the test
@Test
@Skip()
@DisplayName("This test will be skipped")
fun testToBeSkipped() {
    // This code will not be executed
}

// Skip the test only on Android
@Test
@Skip(platform = "android")
@DisplayName("Test is skipped only on Android")
fun testSkipOnAndroid() {
    // This code will run only on iOS
}

// Skip the test only on iOS
@Test
@Skip(platform = "ios")
@DisplayName("Test is skipped only on iOS")
fun testSkipOnIOS() {
    // This code will run only on Android
}
```

The `@Skip` annotation is useful for:
- Temporarily disabling tests that are under development
- Creating platform-specific tests that should run only on a single platform
- Skipping tests that depend on functionality available only on a specific platform

### Special Blocks for Organizing Tests
The framework provides special blocks to organize tests:

- `context.run { ... }` — Main execution block of a test
- `optionalAndroid({ ... })` — Code block executed only on Android
- `optionalIos({ ... })` — Code block executed only on iOS
- `optional({ ... })` — Optional code block; if it fails, the test won’t fail (useful for elements that may not always appear)
- `onlyAndroid { ... }` — Code block executed only on Android
- `onlyIos { ... }` — Code block executed only on iOS

#### Example Test Structure
```kotlin
class ExampleTest : MobileTest() {

    @Test
    @DisplayName("Test Name")
    @Description("A description of what the test should do if the title is not sufficient")
    @Feature("Feature Name")
    fun openAppExample() {
        context.run {
            "Loading the app's start screen" {
                // ...
                "Assertion 1" {
                    // ...
                }
            }

            onlyAndroid {
                "Step executed only on Android" {
                    // ...
                    "Assertion 1" {
                        // ...
                    }
                }
            }

            onlyIos {
                "Step executed only on iOS" {
                    // ...
                    "Assertion 1" {
                        // ...
                    }
                }
            }

            optionalAndroid(
                {
                    "Step executed only on Android" {
                        // ...
                        "Assertion 1" {
                            // ...
                        }
                    }
                }
            )

            optionalIos(
                {
                    "Step executed only on iOS" {
                        // ...
                        "Assertion 1" {
                            // ...
                        }
                    }
                }
            )

            optional(
                {
                    "Optional step; if it fails, the test will not fail" {
                        // ...
                        "Check display of a widget that may or may not appear (50/50)" {
                            // ...
                        }
                    }
                }
            )
        }
    }
}
```

Example test:
```kotlin
class SmokeTest : MobileTest() {

    @Test
    @DisplayName("Test Name")
    @Description("A description of what the test should do if the title is not sufficient")
    @Feature("Feature name; tests will be grouped by feature in the report")
    fun openAppExample() {
        context.run {
            "App launch" {
                "Region selection screen is displayed" {
                    checkVisible(ExampleScreen.ruRegion)
                }
            }

            "Select region" {
                click(ExampleScreen.ruRegion)

                "'Home' button on the navigation bar is visible" {
                    checkVisible(ExampleScreen.homeNavBar)
                }
                "'Catalog' button on the navigation bar is visible" {
                    checkVisible(ExampleScreen.catNavBar)
                }
                "'Cart' button on the navigation bar is visible" {
                    checkVisible(ExampleScreen.cartNavBar)
                }
                "'Profile' button on the navigation bar is visible" {
                    checkVisible(ExampleScreen.profileNavBar)
                }
            }

            "Walk through the navbar" {
                click(ExampleScreen.profileNavBar)
                click(ExampleScreen.cartNavBar)
                click(ExampleScreen.catNavBar)
                click(ExampleScreen.homeNavBar)
            }

            "Scroll back and forth" {
                scrollDown(scrollCount = 5)
                scrollUp(scrollCount = 2)
            }
        }
    }
}
```

### Test Lifecycle
The framework supports standard JUnit 5 lifecycle methods:

```kotlin
// Runs once before all tests in the class
@BeforeAll
fun setUpAll() {
    // Initialize shared resources
}

// Runs before each test
@BeforeEach
fun setUp(testInfo: TestInfo) {
    // Prepare for the test
    // testInfo contains information about the current test
}

// Runs after each test
@AfterEach
fun tearDown(testInfo: TestInfo) {
    // Wait for all asynchronous event checks
    awaitAllEventChecks()
    // Close the application
    app.close()
    // Save the test video recording
}

// Runs once after all tests in the class
@AfterAll
fun tearDownAll() {
    // Release shared resources
}
```

The base class `MobileTest` already implements these lifecycle methods, which automatically:
- start and stop emulators/simulators (if enabled)
- start and stop video recording (if enabled)
- wait for completion of asynchronous event checks
- close the application after each test

## Page Object Structure

Each method should receive a locator of the UI element on the page, defined in the Page Object model.  
Every element belongs to a specific class in the Page Object model corresponding to a widget, screen, etc.  
The Page Object model should be located in `test/kotlin/uipages/` and organized into packages by feature area.

Example of an element definition in a Page Object model:
```kotlin
object ExampleScreen {

    // Standard element definition with different locators for Android and iOS
    val ruRegionRussiaText = PageElement(
        android = XPath("""//android.widget.TextView[@text="Россия"]"""),
        ios = Value("Россия")
    )

    // Using a list of alternative locators for Android.
    // The framework will try each locator in the list until the element is found.
    val ruRegionRussiaTextWithAlternatives = PageElement(
        androidList = listOf(Text("Russia"), Text("Россия")),
        ios = Name("Россия")
    )

    val homeNavBar = PageElement(
        android = ResourceId("HomeNavBar"),
        ios = Name("Main page")
    )

    val findOfferBar = PageElement(
        android = Text("Найти товар"),
        ios = null  // Element available only on Android
    )

    val favorites = PageElement(
        android = ContentDesc("Добавить в отложенные"),
        ios = Name("IconDual/24/Stroke/heartEmptyWhiteBlack")
    )

    // Using AccessibilityId for both platforms
    val accessebilityIdLocator = PageElement(
        android = AccessibilityId("Поиск"),
        ios = AccessibilityId("Поиск")
    )

    // Using UIAutomator for Android
    val androidUIAutomatorLocator = PageElement(
        android = AndroidUIAutomator("new UiSelector().text(\"Поиск\")")
    )

    // Using Class Chain for iOS
    val iOSClassChainLocator = PageElement(
        ios = IOSClassChain("**/XCUIElementTypeStaticText[`name == \"Поиск\"`]")
    )

    // Using Predicate String for iOS
    val iOSPredicateStringLocator = PageElement(
        ios = IOSPredicateString("name == \"Поиск\"")
    )
}
```

Element search on a page is available by attributes: `id`, `resource-id`, `text`, `contains-text`, `content-desc`, `xpath`, `value`, `name`, `label`, `accessibility-id`, `android-uiautomator`, `ios-class-chain`, `ios-predicate-string`

### Advanced Locator Capabilities

The framework supports several advanced strategies for locating elements:

1. **Alternative locators** — You can provide a list of alternative locators for Android via the `androidList` parameter. The framework will try each locator in order until the element is found.

2. **Platform-specific locators** — You can specify different locators for Android and iOS to write cross-platform tests.

3. **Specialized locators** — Support for platform-specific locators such as `AndroidUIAutomator`, `IOSClassChain`, and `IOSPredicateString` for more flexible element search.

## Method Descriptions (each function includes detailed documentation)

### Data Retrieval Methods
- `getText()` — Get text from an [element] found on the screen
- `getNumber()` — Extract a numeric value from an [element] found on the screen (a universal parser from “noisy” text)

### StepContext
- `click()` — Find an [element] on the screen and tap it
- `click(eventName, eventData)` — Find and tap the element associated with [eventName] and [eventData]
- `typeText()` — Find an [element] on the screen and input [text]
- `tapArea()` — Tap on the screen at coordinates [x], [y]
- `tapElementArea()` — Tap within an [element] at its relative [x], [y]
- `scrollDown()` — Scroll down
- `scrollUp()` — Scroll up
- `scrollRight()` — Scroll right
- `scrollLeft()` — Scroll left
- `swipeDown()` — Swipe down within an [element]
- `swipeUp()` — Swipe up within an [element]
- `swipeRight()` — Swipe right within an [element]
- `swipeLeft()` — Swipe left within an [element]
- `openDeeplink(deeplink)` — Open the provided deeplink URL (supported on Android and iOS)

  ```kotlin
  // Usage examples:
  openDeeplink("project://main")      // Open the main page
  openDeeplink("project://catalog")   // Open the catalog page
  openDeeplink("project://cart")      // Open the cart page
  openDeeplink("project://profile")   // Open the profile page
  ```

### ExpectationContext
- `checkVisible()` — Verify that an [element] is visible on the screen
- `checkHasEvent(eventName, eventData)` — Verifies the presence of event [eventName] with [eventData] in EventStorage
- `checkHasEvent(eventName, eventDataFile)` — Verifies the presence of event [eventName] with data from [eventDataFile] in EventStorage
- `checkHasEventAsync(eventName, eventData)` — Asynchronously verifies event [eventName] with [eventData] in EventStorage based on user actions performed **after** the function call
- `checkHasEventAsync(eventName, eventDataFile)` — Asynchronously verifies event [eventName] with data from [eventDataFile] in EventStorage

### Helper Methods
- `awaitAllEventChecks()` — Waits for completion of all asynchronous event checks (called in `tearDown()`)

### Description of Allowed Parameters (each parameter is documented in detail)
- `element: PageElement?` — the element
- `elementNumber: Int?` — index of the found element starting from 1
- `timeoutBeforeExpectation: Long` — seconds to wait **before** starting to search for the element
- `timeoutExpectation: Long` — number of seconds to keep searching for the element
- `pollingInterval: Long` — polling frequency in milliseconds
- `scrollCount: Int` — number of scroll attempts toward the element if not found on the current screen
- `scrollCapacity: Double` — scroll height modifier `[0.0 - 1.0]`; at `1.0` the screen scrolls by one full page
- `scrollDirection: ScrollDirection` — direction of screen scrolling
- `x: Int`, `y: Int` — screen coordinates for `tapArea()` and `tapElementArea()`
- `eventName: String` — event name to search for in EventStorage
- `eventData: String` — JSON string with data to validate in the event
- `eventDataFile: File` — file containing JSON data to validate in the event
- `deeplink: String` — deeplink string to open in the app

## Working with Events & Analytics
The framework provides powerful capabilities for validating analytics events sent by the app. It uses the `EventStorage` class, which stores all events intercepted by the proxy server.

### Event Processing Architecture

The event pipeline consists of the following components:

1. **Proxy Server** — intercepts the app’s network traffic and extracts analytics events
2. **EventStorage** — stores all intercepted events in memory
3. **EventMatcher** — matches events against expected patterns
4. **EventHandler** — processes events and performs checks

This architecture enables:
- Intercepting events without modifying the application
- Checking events synchronously or asynchronously
- Using flexible patterns to match data
- Interacting with the UI based on events

### Event Verification

The following methods are used to verify events:

| Method | Description | Parameters |
|-------|-------------|------------|
| `checkHasEvent(eventName, eventData)` | Synchronous check for event presence | `eventName`: event name<br>`eventData`: JSON string with data<br>`timeoutEventExpectation`: wait timeout (sec) |
| `checkHasEventAsync(eventName, eventData)` | Asynchronous event check | `eventName`: event name<br>`eventData`: JSON string with data<br>`timeoutEventExpectation`: wait timeout (sec) |
| `checkHasEvent(eventName, eventDataFile)` | Check using data from a file | `eventName`: event name<br>`eventDataFile`: file with JSON data<br>`timeoutEventExpectation`: wait timeout (sec) |
| `checkHasEventAsync(eventName, eventDataFile)` | Asynchronous check using data from a file | `eventName`: event name<br>`eventDataFile`: file with JSON data<br>`timeoutEventExpectation`: wait timeout (sec) |

### Matching Patterns for Values in `eventData`

When validating events, the following patterns are supported for JSON values:

| Pattern | Description | Example |
|---------|-------------|---------|
| `"*"` | Matches any value (wildcard) | `{"user_id": "*"}` |
| `""` | Matches only an empty value | `{"comment": ""}` |
| `"~value"` | Checks for partial match (substring) | `{"product_name": "~iPhone"}` |
| `"^value"` | Checks that the string starts with `value` | `{"url": "^https://"}` |
| `"$value"` | Checks that the string ends with `value` | `{"file_name": "$pdf"}` |
| `"#number"` | Checks for an exact numeric value | `{"price": "#100"}` |
| `"<number"` | Checks that the number is less than the specified value | `{"quantity": "<10"}` |
| `">number"` | Checks that the number is greater than the specified value | `{"rating": ">4"}` |
| Plain value | Checks for exact match | `{"status": "success"}` |

#### Usage Examples

```kotlin
// Check event with any value for "loc"
checkHasEvent(
    eventName = "view_item",
    eventData = """{"loc": "*"}""",
    timeoutEventExpectation = 5  // Wait up to 5 seconds for the event
)

// Check event where "comment" field is empty
checkHasEvent(
    eventName = "add_comment",
    eventData = """{"comment": ""}""",
    timeoutEventExpectation = 5
)

// Check event where "product_name" contains substring "iPhone"
checkHasEvent(
    eventName = "view_item",
    eventData = """{"product_name": "~iPhone"}""",
    timeoutEventExpectation = 5
)

// Check event where "url" starts with https://
checkHasEvent(
    eventName = "open_url",
    eventData = """{"url": "^https://"}""",
    timeoutEventExpectation = 5
)

// Check event where "file_name" ends with .pdf
checkHasEvent(
    eventName = "download_file",
    eventData = """{"file_name": "$pdf"}""",
    timeoutEventExpectation = 5
)

// Check event with exact numeric value
checkHasEvent(
    eventName = "purchase",
    eventData = """{"price": "#100"}""",
    timeoutEventExpectation = 5
)

// Check event where "quantity" is less than 10
checkHasEvent(
    eventName = "add_to_cart",
    eventData = """{"quantity": "<10"}""",
    timeoutEventExpectation = 5
)

// Check event where "rating" is greater than 4
checkHasEvent(
    eventName = "rate_product",
    eventData = """{"rating": ">4"}""",
    timeoutEventExpectation = 5
)

// Check event with exact match for "status"
checkHasEvent(
    eventName = "api_call",
    eventData = """{"status": "success"}""",
    timeoutEventExpectation = 5
)

// Combine multiple patterns in one event
checkHasEvent(
    eventName = "purchase",
    eventData = """
        {
            "product_id": "*",
            "product_name": "~iPhone",
            "price": ">500",
            "currency": "USD",
            "quantity": "<5"
        }
    """,
    timeoutEventExpectation = 5
)
```

#### Synchronous Event Verification

A synchronous check blocks test execution until the event is received or the timeout expires:

```kotlin
"Verify event is sent when tapping a product" {
    // Perform the action
    click(MobileExamplePage.productCard)
    
    // Verify that the event was sent
    checkHasEvent(
        eventName = "product_click",
        eventData = """
            {
                "product_id": "12345",
                "category": "electronics",
                "position": "#1",
                "list_name": "featured_products"
            }
        """,
        timeoutEventExpectation = 5  // Wait for the event up to 5 seconds
    )
}
```

#### Asynchronous Event Verification

An asynchronous check does **not** block test execution, allowing you to continue interacting with the app:

```kotlin
"Verify event is sent when tapping a product (asynchronously)" {
    // Start the asynchronous check
    checkHasEventAsync(
        eventName = "product_click",
        eventData = """
            {
                "product_id": "12345",
                "category": "electronics"
            }
        """,
        timeoutEventExpectation = 10  // Wait up to 10 seconds for the event
    )

    // Continue the test without waiting for the event check
    click(MobileExamplePage.productCard)
    click(MobileExamplePage.addToCartButton)
    // ...
}
```

When using asynchronous checks, you must call `awaitAllEventChecks()` to wait for all checks to complete. You can call this method:

1. **In the `tearDown()` method** — to automatically wait for all asynchronous checks at the end of each test:

```kotlin
@AfterEach
fun tearDown() {
    // Wait for all asynchronous event checks to complete
    awaitAllEventChecks()
    // Close the application
    app.close()
}
```

2. **Inside the test** — if you need to wait for asynchronous checks to complete at a specific point:

```kotlin
"Check event presence asynchronously" {
    // Start the asynchronous check
    checkHasEventAsync(
        eventName = "App_Start",
        timeoutEventExpectation = 10
    )

    // Perform other actions
    click(MobileExamplePage.searchBar)

    // Wait for all asynchronous checks to complete at this point in the test
    awaitAllEventChecks()

    // Continue the test execution
}
```

The base class `MobileTest` already calls `awaitAllEventChecks()` in its `tearDown()` method, so in most cases you don’t need to call it explicitly if your tests inherit from `MobileTest`.

#### Verifying Events Using Data from a File

For complex or repeated checks, it’s convenient to store expected data in separate JSON files:

```kotlin
"Verify event with data from a file" {
    checkHasEvent(
        eventName = "purchase_complete",
        eventDataFile = File("src/test/resources/events/purchase_event.json")
    )
}
```

Contents of `purchase_event.json`:
```json
{
  "transaction_id": "*",
  "value": ">100",
  "currency": "USD",
  "items": [
    {
      "item_id": "*",
      "item_name": "~iPhone",
      "price": "*"
    }
  ]
}
```

### Interacting with Elements Based on Events

The framework allows you to locate and interact with elements based on the analytics events associated with them:

```kotlin
"Tap the item that emits a specific event" {
    click(
        eventName = "view_item_in_list",
        eventData = """{"loc": "SNS"}""",
        scrollCount = 1,
        eventPosition = "first"  // You can specify "first" or "last"
    )
}
```

This approach enables more flexible tests that can adapt to UI changes while maintaining a strong link to business logic through analytics events.

#### Advanced Interaction with Elements Based on Events

```kotlin
// Click the first element associated with the specified event
click(
    eventName = "view_item_in_list",
    eventData = """{"category": "electronics"}""",
    eventPosition = "first"
)

// Click the last element associated with the specified event
click(
    eventName = "view_item_in_list",
    eventData = """{"category": "electronics"}""",
    eventPosition = "last"
)

// Click with scrolling to the element
click(
    eventName = "view_item_in_list",
    eventData = """{"category": "electronics"}""",
    scrollCount = 3,  // Scroll up to 3 times if the element is not visible
    eventPosition = "first"
)

// Type text into a field associated with the event
typeText(
    eventName = "search_query",
    eventData = """{"source": "main_page"}""",
    text = "iPhone 16",
    eventPosition = "first"
)
```

### Event Debugging

For debugging events, you can use the `printAllEvents()` method, which prints all intercepted events to the console:

```kotlin
"Event debugging" {
    // Perform actions
    click(MobileExamplePage.productCard)

    // Print all events to the console
    printAllEvents()
}
```

You can also use the `getEventCount(eventName)` method to get the number of events with the specified name:

```kotlin
"Verify the number of events" {
    // Perform actions
    click(MobileExamplePage.productCard)
    
    // Verify that exactly one event was sent
    val count = getEventCount("product_click")
    assert(count == 1) { "Expected 1 event, got $count" }
}
```

## Allure
During test runs, reports are generated and recorded in Allure. The framework provides extended Allure integration to improve the organization and visualization of test reports.

### Key Features of Allure Integration

- Automatic generation of test execution reports
- Grouping tests by features, epics, and stories using annotations `@Feature`, `@Epic`, `@Story`
- Detailed test descriptions using annotations `@DisplayName` and `@Description`
- Organizing tests into suites with the custom annotation `@Suite`
- Attaching screenshots, logs, and video recordings to reports
- Tracking test execution steps with detailed information

### Using the Suite Annotation

To group tests into logical suites, use the `@Suite` annotation. This annotation allows you to organize tests into structured groups in Allure reports, improving navigation and analysis of test results.

```kotlin
@Suite("Suite Name")
@ExtendWith(AllureExtension::class)
class ExampleTest : MobileTest() {
    // Tests
}
```

The `@Suite` annotation works together with `AllureExtension`, which adds custom labels to Allure reports. This enables you to:

- Group tests by functional areas
- Create a hierarchical test structure
- Improve navigation in Allure reports
- Simplify analysis of test results

Example usage with additional annotations:

```kotlin
@Suite("Mobile Application")
@Feature("Cart")
@Story("Adding items to cart")
@ExtendWith(AllureExtension::class)
class CartTest : MobileTest() {
    @Test
    @DisplayName("Add product to cart from product page")
    fun addProductToCartFromProductPage() {
        // Test body
    }

    @Test
    @DisplayName("Add product to cart from product list")
    fun addProductToCartFromProductList() {
        // Test body
    }
}
```

### Report Generation

To generate the report from the command line, run:

```bash
allure serve build/allure-results
```

## Advanced Framework Features

### Automatic Management of Emulators and Simulators

The framework provides built-in support for automatically managing the lifecycle of Android emulators and iOS simulators via the `EmulatorManager` class. This allows emulators/simulators to be started and stopped automatically during test execution.

#### Key Capabilities

- Automatic start of the emulator/simulator before testing begins
- Automatic shutdown of the emulator/simulator after tests complete
- Health checks for the emulator/simulator
- Handling of edge cases and errors
- Support for both Android and iOS platforms

#### Configuration

You can configure the following parameters in `config.properties` or via the command line:

```properties
# Automatically start the emulator/simulator before tests
emulator.auto.start=true
# Automatically shut down the emulator/simulator after tests
emulator.auto.shutdown=true
```

### Parameterized Testing

The framework supports parameterized testing using JUnit 5, allowing the same test to run with different input data. This is especially useful for validating identical functionality with multiple datasets without duplicating code.

#### Key Capabilities

- Parameterizing tests using built-in data sources
- Support for CSV files as test data sources
- Support for JSON files for complex data structures
- Automatic retry of only the failed parameterized cases with the same parameters
- Customizable test names including parameter values
- Combining multiple data sources

#### Data Sources for Parameterization

The framework supports the following data sources:

1. **@ValueSource** — simple values of a single type (strings, numbers, booleans)
2. **@CsvSource** — CSV data directly in the annotation
3. **@CsvFileSource** — data from a CSV file
4. **@MethodSource** — data from a method returning a `Stream` or `Collection`
5. **@EnumSource** — values from an enum
6. **@ArgumentsSource** — custom argument provider

#### Usage Examples

##### Simple values with @ValueSource

```kotlin
@ParameterizedTest(name = "Product search: {0}")
@ValueSource(strings = ["Телефон", "Ноутбук", "Наушники"])
@DisplayName("Search for various products")
fun searchProductTest(searchQuery: String) {
    context.run {
        "Open search" {
            click(MobileExamplePage.searchBar)
        }
        
        "Enter search query: $searchQuery" {
            typeText(MobileExamplePage.searchInput, searchQuery)
        }
        
        "Verify search results" {
            checkVisible(MobileExamplePage.searchResults)
        }
    }
}
```

##### Data from a CSV file

Example `test_data.csv`:
```
search_query,expected_result,min_results
Телефон,Samsung Galaxy,5
Ноутбук,MacBook Pro,3
Наушники,AirPods,2
```

```kotlin
@ParameterizedTest(name = "Search {0} expecting {1} with at least {2} results")
@CsvFileSource(resources = ["/test_data.csv"], numLinesToSkip = 1)
@DisplayName("Search with CSV-driven result checks")
fun searchWithResultsTest(searchQuery: String, expectedResult: String, minResults: Int) {
    context.run {
        "Open search" {
            click(MobileExamplePage.searchBar)
        }

        "Enter search query: $searchQuery" {
            typeText(MobileExamplePage.searchInput, searchQuery)
        }

        "Verify search results" {
            checkVisible(MobileExamplePage.searchResults)

            "Verify expected result is present: $expectedResult" {
                checkVisible(PageElement(
                    android = Text(expectedResult),
                    ios = Name(expectedResult)
                ))
            }

            "Verify number of results (minimum $minResults)" {
                // Validate the number of results
            }
        }
    }
}
```

// Data class for the test
```
data class TestData(val name: String, val param1: String, val param2: Int)

class ParameterizedExampleTest : MobileTest() {
    @ParameterizedTest(name = "Test with method-sourced data: {0}")
    @MethodSource("provideTestData")
    @DisplayName("Parameterized test with complex data")
    fun testWithMethodSource(testData: TestData) {
        context.run {
            "Perform actions with data: ${testData.name}" {
                // Use testData.param1, testData.param2, etc.
            }
        }
    }

    // Method that provides data for the test
    companion object {
        @JvmStatic
        fun provideTestData(): Stream<TestData> {
            return Stream.of(
                TestData("Test1", "param1", 100),
                TestData("Test2", "param2", 200),
                TestData("Test3", "param3", 300)
            )
        }
    }
}
```

##### Combining Data Sources

```kotlin
@ParameterizedTest(name = "Platform: {0}, Language: {1}")
@CsvSource(
    value = [
        "ANDROID, ru",
        "ANDROID, en",
        "iOS, ru",
        "iOS, en"
    ]
)
@DisplayName("Localization test across platforms")
fun testLocalization(platform: String, language: String) {
    // Test body using platform and language
}
```

### Video Recording Settings

The project includes the ability to record video during test execution using the `VideoRecorder` class.  
This allows you to capture videos of test runs on mobile devices for debugging, documentation, and demonstration purposes.

#### Key Features

- Automatic video recording during test execution
- Configurable video size, quality, and bitrate
- Integration with Allure reports
- Support for Android and iOS platforms (with limitations)

#### Available Parameters

You can configure the following parameters in `config.properties` or via the command line:

- `android.video.recording.enabled` — enable/disable video recording for Android (`true`/`false`)
- `ios.video.recording.enabled` — enable/disable video recording for iOS (`true`/`false`)
- `video.recording.size` — video resolution (e.g., `"1280x720"`)
- `video.recording.quality` — video quality (0–100)
- `video.recording.bitrate` — video bitrate (in bits per second)
- `video.recording.output.dir` — directory where videos will be saved

#### Usage

Parameters can be specified at test runtime via the command line using the `-P` flag:

```bash
./gradlew test \
  -Pandroid.video.recording.enabled=true \
  -Pios.video.recording.enabled=true \
  -Pvideo.recording.size=1280x720 \
  -Pvideo.recording.quality=70 \
  -Pvideo.recording.bitrate=1000000
```

If parameters are not specified, the values from the `config.properties` file or the default values will be used:

```properties
android.video.recording.enabled=true
ios.video.recording.enabled=true
video.recording.size=640x360
video.recording.quality=20
video.recording.bitrate=500000
video.recording.output.dir=build/videos
```

#### Example Usage in CI/CD

```yaml
# Example for GitLab CI
test:
  script:
    - ./gradlew test -Pandroid.video.recording.enabled=true -Pios.video.recording.enabled=true -Pvideo.recording.size=1280x720 -Pvideo.recording.quality=70 -Pvideo.recording.output.dir=build/videos -Pvideo.recording.bitrate=500000
  artifacts:
    paths:
      - build/videos/
```

#### Key Features

- Headless mode for running without a graphical interface
- Unified DSL for writing tests
- Integration with Allure for reporting

#### Configuration

You can configure the following parameters in `config.properties` or via the command line:

```properties
# Automatic emulator/simulator startup before tests
emulator.auto.start=true
# Automatic emulator/simulator shutdown after tests
emulator.auto.shutdown=true
```

## API Documentation (Dokka)

This project supports automatic API documentation generation using Dokka.

How to generate:
- Run: `./gradlew dokkaHtml` (or the alias: `./gradlew generateApiDocs`)
- After the task completes, open: `build/dokka/html/index.html`

Notes:
- When publishing artifacts, the `javadocJar` is built from the Dokka HTML output, so consumers get a documentation artifact.
- Dokka tasks do not require additional configuration by default; they scan sources from `src/main/kotlin`.

## Conclusion

The Easy Test Write framework provides a powerful and flexible toolkit for automating tests of mobile and web applications. Key advantages:

- **Cross-platform** — supports Android, iOS, and web applications
- **Rich DSL** — intuitive syntax for writing tests
- **Enhanced Allure integration** — detailed execution reports
- **Automatic emulator management** — start/stop emulators/simulators
- **Video recording** — automatic capture of test runs
- **Analytics validation** — built-in tools for verifying analytics events
- **Parameterized testing** — support for multiple data sources
- **Flexible locators** — advanced element search capabilities

The framework is continuously evolving, with new features being added and existing functionality improved.