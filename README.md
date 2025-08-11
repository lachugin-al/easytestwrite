# Easy test write

Фреймворк для автоматизации тестирования мобильных приложений

![Version](https://img.shields.io/badge/version-0.0.45-blue.svg)

## Содержание
- [Обзор](#обзор)
- [Начало работы](#начало-работы)
  - [Настройка окружения](#настройка-окружения-перед-началом-работы)
  - [Структура проекта](#структура-проекта)
- [Запуск тестов](#запуск-тестов)
  - [Локальный запуск](#локальный-запуск-e2e-тестов)
  - [Запуск с тегами](#запуск-тестов-с-тегами)
  - [Запуск с управлением эмуляторами](#запуск-тестов-с-автоматическим-управлением-эмуляторами)
- [Написание тестов](#написание-тестов)
  - [Структура UI теста](#структура-ui-теста)
  - [Типы тестов](#типы-тестов)
  - [Специальные блоки](#специальные-блоки-для-организации-тестов)
  - [Жизненный цикл теста](#жизненный-цикл-теста)
  - [Пропуск тестов](#пропуск-тестов-с-аннотацией-skip)
- [PageObject модель](#описание-структуры-pageobject)
  - [Локаторы элементов](#расширенные-возможности-локаторов)
- [API фреймворка](#описание-методов-к-каждой-функции-добавлена-документация-с-подробным-описанием)
  - [Методы извлечения данных](#методы-извлечения-данных)
  - [Методы взаимодействия](#stepcontext)
  - [Методы проверки](#expectationcontext)
  - [Вспомогательные методы](#вспомогательные-методы)
  - [Параметры методов](#описание-возможных-параметров-допустимых-для-передачи-в-функции-к-каждой-параметру-добавлена-документация-с-подробным-описанием)
- [Работа с событиями и аналитикой](#работа-с-событиями-и-аналитикой)
  - [Архитектура](#архитектура-работы-с-событиями)
  - [Проверка событий](#проверка-событий)
  - [Шаблоны сопоставления](#шаблоны-для-сопоставления-значений-в-eventdata)
  - [Взаимодействие с элементами](#взаимодействие-с-элементами-на-основе-событий)
  - [Отладка событий](#отладка-событий)
- [Интеграция с Allure](#allure)
  - [Возможности](#основные-возможности-allure-интеграции)
  - [Аннотация Suite](#использование-аннотации-suite)
  - [Генерация отчета](#генерация-отчета)
- [Расширенные возможности](#расширенные-возможности-фреймворка)
  - [Управление эмуляторами](#автоматическое-управление-эмуляторами-и-симуляторами)
  - [Параметризованное тестирование](#параметризованное-тестирование)
  - [Запись видео](#настройки-записи-видео)
- [Заключение](#заключение)

## Обзор

Использование: 
- для автоматизации end-to-end тестирования пользовательских сценариев мобильного приложения
- для автоматизации тестирования аналитики
- для создания параметризованных тестов с различными источниками данных

## Начало работы

### Настройка окружения перед началом работы:
1. Установить `JDK 21` - [инструкция](https://www.oracle.com/java/technologies/downloads/#java21)
2. Открыть `Terminal`
3. Установить переменные среды окружения `ANDROID_SDK_ROOT, ANDROID_HOME, JAVA_HOME`
4. Установить `Appium` - [инструкция](https://github.com/appium/appium)
5. Установить нативные драйвера для Android и iOS: 
   1. Android - [инструкция](https://github.com/appium/appium-uiautomator2-driver)
   2. iOS - [инструкция](https://github.com/appium/appium-xcuitest-driver)
6. Установить `FFmpeg` для записи видео:
   - Mac: `brew install ffmpeg`
   - Linux: `sudo apt install ffmpeg`
   - Windows: `choco install ffmpeg` или `winget install ffmpeg`
7. Открыть проект в `Android Studio`
8. Перейти в `Device Manager` и нажать + `Create Virtual Device`, далее по шагам выбрать `Pixel 2` и `API 34`
9. Установить `Xcode` (только для macOS)
10. Открыть `Xcode`, в меню выбрать `Open Developer Tool -> Simulator`, далее создать `iPhone 16 Plus`

### Структура проекта
Вся кодовая база находится в папке `src/main/` и разделена по пакетам:
* `app` - классы для подключения и запуска приложения для тестирования
  * `config` - классы конфигурации
  * `driver` - классы драйверов для Android и iOS
  * `model` - модели данных
* `controller` - классы с контроллерами
  * `mobile` - контроллеры для мобильных тестов
  * `element` - классы для работы с элементами UI
  * `handler` - обработчики специфических взаимодействий (алерты и др.)
* `dsl` - классы DSL для написания тестов
* `events` - классы работы с событиями и аналитикой
* `proxy` - классы прокси сервера для перехвата и анализа сетевого трафика
* `utils` - классы с утилитами
  * `allure` - расширения для Allure отчетов
  * `video` - классы для записи видео тестов

Все тесты находятся в папке `src/test/` и разделены по пакетам:
* `mobile` - пакеты с тестами
  * `uipages` - классы PageObject модели разметки приложения
  * `uitests` - классы с UI тестами
* `resources` - ресурсы для тестов (конфигурация, тестовые данные)

## Запуск тестов

### Локальный запуск E2E тестов

#### Подготовка к запуску
1. Для мобильных тестов необходимо запустить Appium сервер, в терминале выполнить команду:
   ```bash
   appium server --allow-cors
   ```

#### Локальный запуск E2E тестов на Android и iOS:
1. В папке `test/resources` создать файл конфигурации `config.properties`
2. Заполнить конфигурационные настройки (либо будут применены настройки по умолчанию)
```config.properties
   # URL-адрес сервера Appium для подключения к мобильным устройствам
    appium.url=http://localhost:4723/
    # Платформа для тестирования (ANDROID или iOS)
    platform=ANDROID
    # Версия операционной системы Android для тестирования
    android.version=16
    # Версия операционной системы iOS для тестирования
    ios.version=18.5
    # Название устройства Android для эмуляции
    android.device.name=Pixel_XL
    # Название устройства iOS для эмуляции
    ios.device.name=iPhone 16 Plus
    # Имя файла приложения для Android
    android.app.name=android.apk
    # Имя файла приложения для iOS
    ios.app.name=ios.app
    # Основная активность приложения Android
    app.activity=MainActivity
    # Идентификатор пакета приложения Android
    app.package=com.ru.dev
    # Автоматическое принятие всплывающих уведомлений на iOS
    ios.auto_accept_alerts=true
    # Автоматическое отклонение всплывающих уведомлений на iOS
    ios.auto_dismiss_alerts=false
    # Режим работы Android-эмулятора без графического интерфейса
    android.headless.mode=true

    # Включение/отключение записи видео для тестов на Android
    android.video.recording.enabled=false
    # Включение/отключение записи видео для тестов на iOS
    ios.video.recording.enabled=false
    # Размер видеозаписи тестов
    video.recording.size=640x360
    # Качество видеозаписи тестов (от 1 до 100)
    video.recording.quality=20
    # Битрейт видеозаписи тестов в битах в секунду
    video.recording.bitrate=50000
    # Директория для сохранения видеозаписей тестов
    video.recording.output.dir=build/videos

    # Настройки автоматического запуска и выключения эмулятора/симулятора
    # Автоматический запуск эмулятора/симулятора перед тестами
    emulator.auto.start=true
    # Автоматическое выключение эмулятора/симулятора после тестов
    emulator.auto.shutdown=true
```
3. Выбрать папку с тестами или отдельные тесты для запуска, вызвать меню и нажать `Run ...`

### Запуск тестов с тегами
Для запуска тестов с определенными тегами используйте параметр `-Ptag`:

```bash
./gradlew test -Ptag=Smoke,Regression
```

Это запустит только тесты, помеченные аннотацией `@Tag("Smoke")` или `@Tag("Regression")`.

### Запуск тестов с автоматическим управлением эмуляторами
Для автоматического запуска и остановки эмуляторов/симуляторов:

```bash
./gradlew test -Pemulator.auto.start=true -Pemulator.auto.shutdown=true
```

## Написание тестов

### Структура UI теста
Тесты должны находиться в папке `test/kotlin/uitests/` и использовать PageObject модель с разметкой элементов.
Тесты необходимо разделять по пакетам, в зависимости от того, к какому функционалу относится тот или иной тест.
Каждый тест должен помечаться аннотацией `@Test`.

### Типы тестов
Фреймворк поддерживает несколько типов тестов:

1. **Мобильные тесты** - наследуются от класса `MobileTest` и используются для тестирования мобильных приложений на Android и iOS
3. **Параметризованные тесты** - используют аннотации `@ParameterizedTest` и различные источники данных
4. **Тесты с тегами** - помечаются аннотацией `@Tag` для выборочного запуска

### Пропуск тестов с аннотацией Skip
Фреймворк предоставляет аннотацию `@Skip` для условного пропуска тестов:

```kotlin
// Пропустить тест безусловно
@Test
@Skip()
@DisplayName("Этот тест будет пропущен")
fun testToBeSkipped() {
    // Этот код не будет выполнен
}

// Пропустить тест только на Android
@Test
@Skip(platform = "android")
@DisplayName("Тест пропускается только на Android")
fun testSkipOnAndroid() {
    // Этот код будет выполнен только на iOS
}

// Пропустить тест только на iOS
@Test
@Skip(platform = "ios")
@DisplayName("Тест пропускается только на iOS")
fun testSkipOnIOS() {
    // Этот код будет выполнен только на Android
}
```

Аннотация `@Skip` полезна для:
- Временного отключения тестов, которые находятся в разработке
- Создания платформо-специфичных тестов, которые должны выполняться только на одной платформе
- Пропуска тестов, которые зависят от функциональности, доступной только на определенной платформе

### Специальные блоки для организации тестов
Фреймворк предоставляет специальные блоки для организации тестов:

- `context.run { ... }` - Основной блок выполнения теста
- `optionalAndroid({ ... })` - Блок кода, который выполняется только на платформе Android
- `optionalIos({ ... })` - Блок кода, который выполняется только на платформе iOS
- `optional({ ... })` - Опциональный блок кода, если не выполнится, то тест не упадет (полезно для проверки элементов, которые могут отображаться не всегда)
- `onlyAndroid { ... }` - Блок кода, который выполняется только на платформе Android
- `onlyIos { ... }` - Блок кода, который выполняется только на платформе iOS

Пример структуры теста:
```kotlin
class ExampleTest : MobileTest() {

    @Test
    @DisplayName("Название теста")
    @Description("Описание того, что должен делать тест, если не достаточно информации из названия")
    @Feature("Название фичи")
    fun openAppExample() {
        context.run {
            "Загрузка стартового экрана приложения" {
              // ...
              "Проверка 1" {
                 // ...
              }
            }

            onlyAndroid {
                "Шаг выполняемый только на Android" {
                    // ...
                    "Проверка 1" {
                        // ...
                    }
                }
            }

            onlyIos {
                "Шаг выполняемый только на iOS" {
                    // ...
                    "Проверка 1" {
                        // ...
                    }
                }
            }

            optionalAndroid(
                {
                    "Шаг выполняемый только на Android" {
                        // ...
                        "Проверка 1" {
                            // ...
                        }
                    }
                }
            )

            optionalIos(
                {
                    "Шаг выполняемый только на iOS" {
                        // ...
                        "Проверка 1" {
                            // ...
                        }
                    }
                }
            )

            optional(
                {
                    "Опциональный шаг, если не выполнится то тест не упадет" {
                        // ...
                        "Проверка отображения виджета который 50/50 отобразится" {
                            // ...
                        }
                    }
                }
            )
        }
    }
}
```

Пример теста:
```kotlin
class SmokeTest : MobileTest() {

    @Test
    @DisplayName("Название теста")
    @Description("Описание того, что должен делать тест, если не достаточно информации из названия")
    @Feature("Название фичи, тесты будут группироваться в отчете по фичам")
    fun openAppExample() {
        context.run {
            "Загрузка приложения" {
                "Экран с выбором региона отображается" {
                    checkVisible(ExampleScreen.ruRegion)
                }
            }

            "Выбрать регион" {
                click(ExampleScreen.ruRegion)

                "Кнопка 'Home' на навигационной панели отображается" {
                    checkVisible(ExampleScreen.homeNavBar)
                }
                "Кнопка 'Catalog' на навигационной панели отображается" {
                    checkVisible(ExampleScreen.catNavBar)
                }
                "Кнопка 'Cart' на навигационной панели отображается" {
                    checkVisible(ExampleScreen.cartNavBar)
                }
                "Кнопка 'Profile' на навигационной панели отображается" {
                    checkVisible(ExampleScreen.profileNavBar)
                }
            }

            "Пробежимся по навбару" {
                click(ExampleScreen.profileNavBar)
                click(ExampleScreen.cartNavBar)
                click(ExampleScreen.catNavBar)
                click(ExampleScreen.homeNavBar)
            }

            "Проскроллим туда-сюда" {
                scrollDown(scrollCount = 5)
                scrollUp(scrollCount = 2)
            }
        }
    }
}
```

### Жизненный цикл теста
Фреймворк поддерживает стандартные методы жизненного цикла JUnit 5:

```kotlin
// Выполняется один раз перед всеми тестами в классе
@BeforeAll
fun setUpAll() {
    // Инициализация общих ресурсов
}

// Выполняется перед каждым тестом
@BeforeEach
fun setUp(testInfo: TestInfo) {
    // Подготовка к тесту
    // testInfo содержит информацию о текущем тесте
}

// Выполняется после каждого теста
@AfterEach
fun tearDown(testInfo: TestInfo) {
    // Ожидает все асинхронные проверки по ивентам
    awaitAllEventChecks()
    // Закрытие приложения
    app.close()
    // Сохранение видеозаписи теста
}

// Выполняется один раз после всех тестов в классе
@AfterAll
fun tearDownAll() {
    // Освобождение общих ресурсов
}
```

Базовый класс `MobileTest` уже содержит реализацию этих методов, которые автоматически:
- Запускают и останавливают эмуляторы/симуляторы (если включено)
- Начинают и останавливают запись видео (если включено)
- Ожидают завершения асинхронных проверок событий
- Закрывают приложение после теста

## Описание структуры PageObject
В каждый метод нам необходимо передавать локатор элемента на странице который описывается в PageObject модели.
Каждый элемент относится к определенному классу в PageObject модели соответствующий определенному виджету, экрану и т.д.
PageObject модель должна располагаться в папке `test/kotlin/uipages/` и разделена по пакетам, которые относятся к тому или иному функционалу.

Пример описания элемента в PageObject модели:
```kotlin
object ExampleScreen {

    // Стандартное определение элемента с разными локаторами для Android и iOS
    val ruRegionRussiaText = PageElement(
        android = XPath("""//android.widget.TextView[@text="Россия"]"""),
        ios = Value("Россия")
    )

    // Использование списка альтернативных локаторов для Android
    // Фреймворк попробует найти элемент по каждому локатору из списка
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
        ios = null  // Элемент доступен только на Android
    )

    val favorites = PageElement(
        android = ContentDesc("Добавить в отложенные"),
        ios = Name("IconDual/24/Stroke/heartEmptyWhiteBlack")
    )

    // Использование AccessibilityId для обеих платформ
    val accessebilityIdLocator = PageElement(
        android = AccessibilityId("Поиск"),
        ios = AccessibilityId("Поиск")
    )

    // Использование UIAutomator для Android
    val androidUIAutomatorLocator = PageElement(
        android = AndroidUIAutomator("new UiSelector().text(\"Поиск\")")
    )

    // Использование Class Chain для iOS
    val iOSClassChainLocator = PageElement(
        ios = IOSClassChain("**/XCUIElementTypeStaticText[`name == \"Поиск\"`]")
    )

    // Использование Predicate String для iOS
    val iOSPredicateStringLocator = PageElement(
        ios = IOSPredicateString("name == \"Поиск\"")
    )
}
```

Поиск элементов на странице доступно по аттрибутам: `id`, `resource-id`, `text`, `contains-text`, `content-desc`, `xpath`, `value`, `name`, `label`, `accessibility-id`, `android-uiautomator`, `ios-class-chain`, `ios-predicate-string`

### Расширенные возможности локаторов

Фреймворк поддерживает несколько продвинутых стратегий локации элементов:

1. **Альтернативные локаторы** - Возможность указать список альтернативных локаторов для Android с помощью параметра `androidList`. Фреймворк будет последовательно пытаться найти элемент по каждому локатору из списка.

2. **Платформо-зависимые локаторы** - Возможность указать разные локаторы для Android и iOS, что позволяет писать кросс-платформенные тесты.

3. **Специализированные локаторы** - Поддержка платформо-специфичных локаторов, таких как AndroidUIAutomator, IOSClassChain и IOSPredicateString, для более гибкого поиска элементов.

## Описание методов (к каждой функции добавлена документация с подробным описанием)

### Методы извлечения данных
- `getText()` - Получить текст из [element] найденном на экране
- `getPrice()` - Получить цену из [element] найденном на экране
- `getAttributeValue()` - Получить значение [attribute] из [element] найденном на экране

### StepContext
- `click()` - Найти элемент на экране по его [element] и кликнуть по нему
- `click(eventName, eventData)` - Найти и кликнуть по элементу, связанному с событием [eventName] и данными [eventData]
- `typeText()` - Найти элемент на экране по его [element] и ввести [text]
- `tapArea()` - Нажать в области экрана по [x] и [y]
- `tapElementArea()` - Нажать в области [element] по его [x] и [y]
- `scrollDown()` - Выполнить скроллирование вниз
- `scrollUp()` - Выполнить скроллирование вверх
- `scrollRight()` - Выполнить скроллирование вправо
- `scrollLeft()` - Выполнить скроллирование влево
- `swipeDown()` - Выполнить свайп в [element] вниз
- `swipeUp()` - Выполнить свайп в [element] вверх
- `swipeRight()` - Выполнить свайп в [element] вправо
- `swipeLeft()` - Выполнить свайп в [element] влево
- `openDeeplink(deeplink)` - Выполняет открытие переданной в параметры ссылки диплинка (поддерживается для Android и iOS)
  ```kotlin
  // Примеры использования:
  openDeeplink("project://main")      // Открытие главной страницы
  openDeeplink("project://catalog")   // Открытие страницы каталога
  openDeeplink("project://cart")      // Открытие страницы корзины
  openDeeplink("project://profile")   // Открытие страницы профиля
  ```

### ExpectationContext
- `checkVisible()` - Проверить виден ли [element] на экране
- `checkHasEvent(eventName, eventData)` - Проверяет наличие события [eventName] и данных [eventData] в EventStorage
- `checkHasEvent(eventName, eventDataFile)` - Проверяет наличие события [eventName] и данных из файла [eventDataFile] в EventStorage
- `checkHasEventAsync(eventName, eventData)` - Проверяет событие [eventName] и данные [eventData] асинхронно в EventStorage исходя из производимых после вызова функции действий пользователя
- `checkHasEventAsync(eventName, eventDataFile)` - Проверяет событие [eventName] и данные из файла [eventDataFile] асинхронно в EventStorage

### Вспомогательные методы
- `awaitAllEventChecks()` - Ожидает завершения всех асинхронных проверок событий (вызывается в методе tearDown())

### Описание возможных параметров, допустимых для передачи в функции (к каждой параметру добавлена документация с подробным описанием):
- `element: PageElement?` - элемент
- `elementNumber: Int?` - номер найденного элемента начиная с 1
- `timeoutBeforeExpectation: Long` - количество секунд, до того как будет производиться поиск элемента
- `timeoutExpectation: Long` - количество секунд в течение которого производится поиск элемента
- `pollingInterval: Long` - частота опроса элемента в миллисекундах
- `scrollCount: Int` - количество скроллирований до элемента, если элемент не найден на текущей странице
- `scrollCapacity: Double` - модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу
- `scrollDirection: ScrollDirection` - направление скроллирования экрана
- `x: Int`, `y: Int` - передача точки `x` и `y` на экране для функций `tapArea()` и `tapElementArea()`
- `eventName: String` - название события для поиска в EventStorage
- `eventData: String` - JSON-строка с данными для проверки в событии
- `eventDataFile: File` - файл с JSON-данными для проверки в событии
- `deeplink: String` - строка с диплинком для открытия в приложении

## Работа с событиями и аналитикой
Фреймворк предоставляет мощные возможности для проверки событий аналитики, отправляемых приложением. Для этого используется класс `EventStorage`, который хранит все события, перехваченные прокси-сервером.

### Архитектура работы с событиями

Система работы с событиями состоит из следующих компонентов:

1. **Прокси-сервер** - перехватывает сетевой трафик приложения и извлекает события аналитики
2. **EventStorage** - хранит все перехваченные события в памяти
3. **EventMatcher** - сопоставляет события с ожидаемыми шаблонами
4. **EventHandler** - обрабатывает события и выполняет проверки

Эта архитектура позволяет:
- Перехватывать события без модификации приложения
- Проверять события синхронно или асинхронно
- Использовать гибкие шаблоны для сопоставления данных
- Взаимодействовать с UI на основе событий

### Проверка событий

Для проверки событий используются следующие методы:

| Метод | Описание | Параметры |
|-------|----------|-----------|
| `checkHasEvent(eventName, eventData)` | Синхронная проверка наличия события | `eventName`: имя события<br>`eventData`: JSON-строка с данными<br>`timeoutEventExpectation`: таймаут ожидания (сек) |
| `checkHasEventAsync(eventName, eventData)` | Асинхронная проверка события | `eventName`: имя события<br>`eventData`: JSON-строка с данными<br>`timeoutEventExpectation`: таймаут ожидания (сек) |
| `checkHasEvent(eventName, eventDataFile)` | Проверка с данными из файла | `eventName`: имя события<br>`eventDataFile`: файл с JSON-данными<br>`timeoutEventExpectation`: таймаут ожидания (сек) |
| `checkHasEventAsync(eventName, eventDataFile)` | Асинхронная проверка с данными из файла | `eventName`: имя события<br>`eventDataFile`: файл с JSON-данными<br>`timeoutEventExpectation`: таймаут ожидания (сек) |

### Шаблоны для сопоставления значений в eventData

При проверке событий поддерживаются следующие шаблоны для значений в JSON:

| Шаблон | Описание | Пример |
|--------|----------|--------|
| `"*"` | Соответствует любому значению (wildcard) | `{"user_id": "*"}` |
| `""` | Соответствует только пустому значению | `{"comment": ""}` |
| `"~value"` | Проверяет частичное совпадение (подстрока) | `{"product_name": "~iPhone"}` |
| `"^value"` | Проверяет начало строки | `{"url": "^https://"}` |
| `"$value"` | Проверяет конец строки | `{"file_name": "$pdf"}` |
| `"#number"` | Проверяет числовое значение | `{"price": "#100"}` |
| `"<number"` | Проверяет, что число меньше указанного | `{"quantity": "<10"}` |
| `">number"` | Проверяет, что число больше указанного | `{"rating": ">4"}` |
| Обычное значение | Проверяет точное соответствие | `{"status": "success"}` |

#### Примеры использования шаблонов

```kotlin
// Проверка события с любым значением для поля "loc"
checkHasEvent(
    eventName = "view_item",
    eventData = """{"loc": "*"}""",
    timeoutEventExpectation = 5  // Ждем событие 5 секунд
)

// Проверка события с пустым значением для поля "comment"
checkHasEvent(
    eventName = "add_comment",
    eventData = """{"comment": ""}""",
    timeoutEventExpectation = 5
)

// Проверка события, где значение поля "product_name" содержит подстроку "iPhone"
checkHasEvent(
    eventName = "view_item",
    eventData = """{"product_name": "~iPhone"}""",
    timeoutEventExpectation = 5
)

// Проверка события, где URL начинается с https://
checkHasEvent(
    eventName = "open_url",
    eventData = """{"url": "^https://"}""",
    timeoutEventExpectation = 5
)

// Проверка события, где имя файла заканчивается на .pdf
checkHasEvent(
    eventName = "download_file",
    eventData = """{"file_name": "$pdf"}""",
    timeoutEventExpectation = 5
)

// Проверка события с точным числовым значением
checkHasEvent(
    eventName = "purchase",
    eventData = """{"price": "#100"}""",
    timeoutEventExpectation = 5
)

// Проверка события, где количество меньше 10
checkHasEvent(
    eventName = "add_to_cart",
    eventData = """{"quantity": "<10"}""",
    timeoutEventExpectation = 5
)

// Проверка события, где рейтинг больше 4
checkHasEvent(
    eventName = "rate_product",
    eventData = """{"rating": ">4"}""",
    timeoutEventExpectation = 5
)

// Проверка события с точным совпадением значения поля "status"
checkHasEvent(
    eventName = "api_call",
    eventData = """{"status": "success"}""",
    timeoutEventExpectation = 5
)

// Комбинирование нескольких шаблонов
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

#### Синхронная проверка событий

Синхронная проверка блокирует выполнение теста до получения события или истечения таймаута:

```kotlin
"Проверка отправки события при клике на товар" {
    // Выполняем действие
    click(MobileExamplePage.productCard)
    
    // Проверяем, что событие было отправлено
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
        timeoutEventExpectation = 5  // Ждем событие 5 секунд
    )
}
```

#### Асинхронная проверка событий

Асинхронная проверка не блокирует выполнение теста, что позволяет продолжать взаимодействие с приложением:

```kotlin
"Проверка отправки события при клике на товар (асинхронно)" {
    // Запускаем асинхронную проверку
    checkHasEventAsync(
        eventName = "product_click",
        eventData = """
            {
                "product_id": "12345",
                "category": "electronics"
            }
        """,
        timeoutEventExpectation = 10  // Ждем событие 10 секунд
    )

    // Продолжаем выполнение теста без ожидания проверки события
    click(MobileExamplePage.productCard)
    click(MobileExamplePage.addToCartButton)
    // ...
}
```

При использовании асинхронных проверок необходимо вызвать метод `awaitAllEventChecks()` для ожидания завершения всех проверок. Этот метод можно вызывать:

1. **В методе `tearDown()`** - для автоматического ожидания всех асинхронных проверок в конце теста:

```kotlin
@AfterEach
fun tearDown() {
    // Ожидаем завершения всех асинхронных проверок
    awaitAllEventChecks()
    // Закрываем приложение
    app.close()
}
```

2. **Внутри теста** - если нужно дождаться завершения асинхронных проверок в определенной точке теста:

```kotlin
"Проверка наличия события асинхронно" {
    // Запускаем асинхронную проверку
    checkHasEventAsync(
        eventName = "App_Start",
        timeoutEventExpectation = 10
    )
    
    // Выполняем другие действия
    click(MobileExamplePage.searchBar)
    
    // Ожидаем завершения всех асинхронных проверок в этой точке теста
    awaitAllEventChecks()
    
    // Продолжаем выполнение теста
}
```

Базовый класс `MobileTest` уже содержит вызов `awaitAllEventChecks()` в методе `tearDown()`, поэтому в большинстве случаев вам не нужно явно вызывать этот метод, если вы наследуетесь от `MobileTest`.

#### Проверка событий с данными из файла

Для сложных или повторяющихся проверок удобно хранить ожидаемые данные в отдельных JSON-файлах:

```kotlin
"Проверка события с данными из файла" {
    checkHasEvent(
        eventName = "purchase_complete",
        eventDataFile = File("src/test/resources/events/purchase_event.json")
    )
}
```

Содержимое файла `purchase_event.json`:
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

### Взаимодействие с элементами на основе событий

Фреймворк позволяет находить и взаимодействовать с элементами на основе связанных с ними событий аналитики:

```kotlin
"Нажимаем на товар, который отправляет определенное событие" {
    click(
        eventName = "view_item_in_list", 
        eventData = """{"loc": "SNS"}""", 
        scrollCount = 1, 
        eventPosition = "first"  // Можно указать "first" или "last"
    )
}
```

Это позволяет создавать более гибкие тесты, которые могут адаптироваться к изменениям в UI, но сохраняют привязку к бизнес-логике через события аналитики.

#### Расширенные возможности взаимодействия с элементами на основе событий

```kotlin
// Клик по первому элементу, связанному с указанным событием
click(
    eventName = "view_item_in_list",
    eventData = """{"category": "electronics"}""",
    eventPosition = "first"
)

// Клик по последнему элементу, связанному с указанным событием
click(
    eventName = "view_item_in_list",
    eventData = """{"category": "electronics"}""",
    eventPosition = "last"
)

// Клик с прокруткой до элемента
click(
    eventName = "view_item_in_list",
    eventData = """{"category": "electronics"}""",
    scrollCount = 3,  // Прокрутить до 3 раз, если элемент не виден
    eventPosition = "first"
)

// Ввод текста в поле, связанное с событием
typeText(
    eventName = "search_query",
    eventData = """{"source": "main_page"}""",
    text = "iPhone 16",
    eventPosition = "first"
)
```

### Отладка событий

Для отладки событий можно использовать метод `printAllEvents()`, который выводит все перехваченные события в консоль:

```kotlin
"Отладка событий" {
    // Выполняем действия
    click(MobileExamplePage.productCard)
    
    // Выводим все события в консоль
    printAllEvents()
}
```

Также можно использовать метод `getEventCount(eventName)` для получения количества событий с указанным именем:

```kotlin
"Проверка количества событий" {
    // Выполняем действия
    click(MobileExamplePage.productCard)
    
    // Проверяем, что было отправлено ровно одно событие
    val count = getEventCount("product_click")
    assert(count == 1) { "Ожидалось 1 событие, получено $count" }
}
```

## Allure
В ходе прогонов, генерируется отчет и записывается в Allure. Фреймворк предоставляет расширенную интеграцию с Allure для улучшения организации и визуализации тестовых отчетов.

### Основные возможности Allure интеграции

- Автоматическая генерация отчетов о выполнении тестов
- Группировка тестов по фичам, эпикам и историям с помощью аннотаций `@Feature`, `@Epic`, `@Story`
- Детальное описание тестов с помощью аннотаций `@DisplayName` и `@Description`
- Организация тестов в наборы (suites) с помощью кастомной аннотации `@Suite`
- Прикрепление скриншотов, логов и видеозаписей к отчетам
- Отслеживание шагов выполнения теста с подробной информацией

### Использование аннотации Suite

Для группировки тестов в логические наборы используйте аннотацию `@Suite`. Эта аннотация позволяет организовать тесты в структурированные группы в отчетах Allure, что улучшает навигацию и анализ результатов тестирования.

```kotlin
@Suite("Название Suite")
@ExtendWith(AllureExtension::class)
class ExampleTest : MobileTest() {
    // Тесты
}
```

Аннотация `@Suite` работает совместно с `AllureExtension`, который добавляет пользовательские метки в отчеты Allure. Это позволяет:

- Группировать тесты по функциональным областям
- Создавать иерархическую структуру тестов
- Улучшать навигацию в отчетах Allure
- Упрощать анализ результатов тестирования

Пример использования с дополнительными аннотациями:

```kotlin
@Suite("Мобильное приложение")
@Feature("Корзина")
@Story("Добавление товаров в корзину")
@ExtendWith(AllureExtension::class)
class CartTest : MobileTest() {
    @Test
    @DisplayName("Добавление товара в корзину со страницы товара")
    fun addProductToCartFromProductPage() {
        // Тело теста
    }
    
    @Test
    @DisplayName("Добавление товара в корзину из списка товаров")
    fun addProductToCartFromProductList() {
        // Тело теста
    }
}
```

### Генерация отчета

Для генерации отчета в командной строке необходимо выполнить команду:

```bash
allure serve build/allure-results
```

## Расширенные возможности фреймворка

### Автоматическое управление эмуляторами и симуляторами

Фреймворк предоставляет встроенную поддержку автоматического управления жизненным циклом эмуляторов Android и симуляторов iOS через класс `EmulatorManager`. Это позволяет запускать и останавливать эмуляторы/симуляторы автоматически в процессе выполнения тестов.

#### Основные возможности

- Автоматический запуск эмулятора/симулятора перед началом тестирования
- Автоматическая остановка эмулятора/симулятора после завершения тестов
- Проверка работоспособности эмулятора/симулятора
- Обработка краевых случаев и ошибок
- Поддержка платформ Android и iOS

#### Настройка

В `config.properties` или через командную строку можно настроить следующие параметры:

```properties
# Автоматический запуск эмулятора/симулятора перед тестами
emulator.auto.start=true
# Автоматическое выключение эмулятора/симулятора после тестов
emulator.auto.shutdown=true
```

### Параметризованное тестирование

Фреймворк поддерживает параметризованное тестирование с использованием JUnit 5, что позволяет запускать один и тот же тест с разными входными данными. Это особенно полезно для тестирования одного и того же функционала с различными наборами данных без дублирования кода.

#### Основные возможности

- Параметризация тестов с использованием встроенных источников данных
- Поддержка CSV-файлов как источников тестовых данных
- Поддержка JSON-файлов для сложных структур данных
- Автоматический перезапуск только упавших тестов с теми же параметрами
- Настраиваемые имена тестов с включением значений параметров
- Комбинирование нескольких источников данных

#### Источники данных для параметризации

Фреймворк поддерживает следующие источники данных:

1. **@ValueSource** - простые значения одного типа (строки, числа, булевы значения)
2. **@CsvSource** - данные в формате CSV непосредственно в аннотации
3. **@CsvFileSource** - данные из CSV-файла
4. **@MethodSource** - данные из метода, возвращающего Stream или Collection
5. **@EnumSource** - значения из перечисления (enum)
6. **@ArgumentsSource** - пользовательский источник аргументов

#### Примеры использования

##### Простые значения с @ValueSource

```kotlin
@ParameterizedTest(name = "Поиск товара: {0}")
@ValueSource(strings = ["Телефон", "Ноутбук", "Наушники"])
@DisplayName("Поиск различных товаров")
fun searchProductTest(searchQuery: String) {
    context.run {
        "Открываем поиск" {
            click(MobileExamplePage.searchBar)
        }
        
        "Вводим поисковый запрос: $searchQuery" {
            typeText(MobileExamplePage.searchInput, searchQuery)
        }
        
        "Проверяем результаты поиска" {
            checkVisible(MobileExamplePage.searchResults)
        }
    }
}
```

##### Данные из CSV-файла

Пример файла `test_data.csv`:
```
search_query,expected_result,min_results
Телефон,Samsung Galaxy,5
Ноутбук,MacBook Pro,3
Наушники,AirPods,2
```

```kotlin
@ParameterizedTest(name = "Поиск {0} с ожиданием {1} и минимум {2} результатов")
@CsvFileSource(resources = ["/test_data.csv"], numLinesToSkip = 1)
@DisplayName("Поиск с проверкой результатов из CSV")
fun searchWithResultsTest(searchQuery: String, expectedResult: String, minResults: Int) {
    context.run {
        "Открываем поиск" {
            click(MobileExamplePage.searchBar)
        }
        
        "Вводим поисковый запрос: $searchQuery" {
            typeText(MobileExamplePage.searchInput, searchQuery)
        }
        
        "Проверяем результаты поиска" {
            checkVisible(MobileExamplePage.searchResults)
            
            "Проверяем наличие ожидаемого результата: $expectedResult" {
                checkVisible(PageElement(
                    android = Text(expectedResult),
                    ios = Name(expectedResult)
                ))
            }
            
            "Проверяем количество результатов (минимум $minResults)" {
                // Проверка количества результатов
            }
        }
    }
}
```

##### Использование MethodSource для сложных данных

```kotlin
// Класс данных для теста
data class TestData(val name: String, val param1: String, val param2: Int)

class ParameterizedExampleTest : MobileTest() {
    @ParameterizedTest(name = "Тест с данными из метода: {0}")
    @MethodSource("provideTestData")
    @DisplayName("Параметризованный тест со сложными данными")
    fun testWithMethodSource(testData: TestData) {
        context.run {
            "Выполняем действия с данными: ${testData.name}" {
                // Использование testData.param1, testData.param2 и т.д.
            }
        }
    }

    // Метод, предоставляющий данные для теста
    companion object {
        @JvmStatic
        fun provideTestData(): Stream<TestData> {
            return Stream.of(
                TestData("Тест1", "param1", 100),
                TestData("Тест2", "param2", 200),
                TestData("Тест3", "param3", 300)
            )
        }
    }
}
```

##### Комбинирование источников данных

```kotlin
@ParameterizedTest(name = "Платформа: {0}, Язык: {1}")
@CsvSource(value = [
    "ANDROID, ru",
    "ANDROID, en",
    "iOS, ru",
    "iOS, en"
])
@DisplayName("Тест локализации на разных платформах")
fun testLocalization(platform: String, language: String) {
    // Тело теста с использованием platform и language
}
```

### Настройки записи видео

В проект добавлена возможность записи видео во время выполнения тестов с использованием класса `VideoRecorder`. Это позволяет записывать видео выполнения тестов на мобильных устройствах для отладки, документирования и демонстрации работы приложения.

#### Основные возможности

- Автоматическая запись видео во время выполнения тестов
- Настройка размера, качества и битрейта видео
- Интеграция с отчетами Allure
- Поддержка платформ Android и iOS (с ограничениями)

#### Доступные параметры

В `config.properties` или через командную строку можно настроить следующие параметры:

- `android.video.recording.enabled` - включение/отключение записи видео для Android (true/false)
- `ios.video.recording.enabled` - включение/отключение записи видео для iOS (true/false)
- `video.recording.size` - размер записываемого видео (например, "1280x720")
- `video.recording.quality` - качество записываемого видео (0-100)
- `video.recording.bitrate` - битрейт видео (бит/с)
- `video.recording.output.dir` - директория для сохранения видеозаписей

#### Использование

Параметры можно задать при запуске тестов через командную строку с помощью флага `-P`:

```bash
./gradlew test -Pandroid.video.recording.enabled=true -Pios.video.recording.enabled=true -Pvideo.recording.size=1280x720 -Pvideo.recording.quality=70 -Pvideo.recording.bitrate=1000000
```

Если параметры не заданы, будут использованы значения из файла `config.properties` или значения по умолчанию:

```properties
android.video.recording.enabled=true
ios.video.recording.enabled=true
video.recording.size=640x360
video.recording.quality=20
video.recording.bitrate=500000
video.recording.output.dir=build/videos
```

#### Пример использования в CI/CD

```yaml
# Пример для GitLab CI
test:
  script:
    - ./gradlew test -Pandroid.video.recording.enabled=true -Pios.video.recording.enabled=true -Pvideo.recording.size=1280x720 -Pvideo.recording.quality=70 -Pvideo.recording.output.dir=build/videos -Pvideo.recording.bitrate=500000
  artifacts:
    paths:
      - build/videos/
```

#### Основные возможности

- Режим headless для запуска без графического интерфейса
- Единый DSL для написания тестов
- Интеграция с Allure для отчетов

#### Настройка

В `config.properties` или через командную строку можно настроить следующие параметры:

```properties
# Automatic emulator/simulator startup before tests
emulator.auto.start=true
# Automatic emulator/simulator shutdown after tests
emulator.auto.shutdown=true
```

## Заключение

Фреймворк Easy Test Write предоставляет мощный и гибкий инструментарий для автоматизации тестирования мобильных и веб-приложений. Основные преимущества:

- **Кросс-платформенность** - поддержка Android, iOS и веб-приложений
- **Богатый DSL** - интуитивно понятный синтаксис для написания тестов
- **Расширенная интеграция с Allure** - подробные отчеты о выполнении тестов
- **Автоматическое управление эмуляторами** - запуск и остановка эмуляторов/симуляторов
- **Запись видео** - автоматическая запись выполнения тестов
- **Проверка аналитики** - встроенные инструменты для проверки событий аналитики
- **Параметризованное тестирование** - поддержка различных источников данных
- **Гибкие локаторы** - расширенные возможности поиска элементов

Фреймворк постоянно развивается, добавляются новые возможности и улучшается существующий функционал.