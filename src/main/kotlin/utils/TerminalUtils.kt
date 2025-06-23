package utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets

/**
 * Утилитарный класс для взаимодействия с терминалом и устройствами эмуляции/симуляции.
 *
 * Позволяет выполнять команды операционной системы и получать информацию об эмуляторах Android и симуляторах iOS.
 */
object TerminalUtils {

    /**
     * Выполняет системную команду и обрабатывает результат выполнения.
     *
     * @param command Список строк, представляющий команду и её аргументы.
     * @param errorMessage Сообщение об ошибке, выводимое в случае неуспешного выполнения команды.
     * @return `true`, если команда завершилась успешно (код возврата 0), иначе `false`.
     */
    fun runCommand(command: List<String>, errorMessage: String): Boolean {
        return try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            val result = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                println("Команда выполнена успешно: ${command.joinToString(" ")}")
                true
            } else {
                println("$errorMessage: $result")
                false
            }
        } catch (e: Exception) {
            println("$errorMessage: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Получает UDID симулятора iOS по его имени.
     *
     * Использует `xcrun simctl list --json` для получения списка доступных устройств
     * и находит среди них активный (Booted) симулятор с заданным именем.
     *
     * @param simulatorName Имя симулятора (например, "iPhone 15 Pro Max").
     * @return UDID симулятора или `null`, если устройство не найдено.
     */
    fun getSimulatorId(simulatorName: String): String? {
        try {
            // Выполнение команды 'xcrun simctl list --json' для получения списка симуляторов в формате JSON
            val process = Runtime.getRuntime().exec("xcrun simctl list --json")
            val output = process.inputStream.bufferedReader(StandardCharsets.UTF_8).readText()

            // Использование kotlinx.serialization для парсинга JSON-ответа с игнорированием неизвестных ключей
            val json = Json {
                ignoreUnknownKeys = true
            }
            val simulatorsResponse = json.decodeFromString<SimulatorsResponse>(output)

            // Поиск нужного симулятора по имени и состоянию 'Booted'
            for (runtime in simulatorsResponse.devices.values) {
                val simulator = runtime.find { it.name == simulatorName && it.state == "Booted" }
                if (simulator != null) {
                    // Возвращаем UDID найденного симулятора
                    return simulator.udid
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Выполняет системную команду и возвращает результат выполнения в виде строки.
     *
     * @param command Список строк, представляющий команду и её аргументы.
     * @return Строка с результатом выполнения команды.
     */
    fun runCommand(command: List<String>): String {
        return try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            val result = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Получает ID эмулятора Android по его имени.
     *
     * Использует `adb devices` для получения списка всех подключённых устройств и выбирает нужный эмулятор.
     *
     * @param emulatorName Имя эмулятора (по умолчанию "emulator-5554").
     * @return ID эмулятора или `null`, если устройство не найдено.
     */
    fun getEmulatorId(emulatorName: String = "emulator-5554"): String? {
        // Запуск команды `adb devices` для получения списка подключенных устройств и эмуляторов
        val command = listOf("adb", "devices")
        val errorMessage = "Не удалось получить ID эмулятора"

        return if (runCommand(command, errorMessage)) {
            val output = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
                .inputStream
                .bufferedReader()
                .use { it.readText() }

            // Поиск ID эмулятора с именем `emulatorName`
            output.lines().firstOrNull { line ->
                line.contains(emulatorName) && line.contains("device")
            }?.split("\t")?.firstOrNull()
        } else {
            null
        }
    }

    /**
     * Модель ответа при запросе списка симуляторов через `xcrun simctl list --json`.
     *
     * @property devices Карта, где ключ — название версии платформы, значение — список симуляторов.
     */
    @Serializable
    data class SimulatorsResponse(val devices: Map<String, List<Simulator>>)

    /**
     * Модель симулятора iOS.
     *
     * @property udid Уникальный идентификатор устройства.
     * @property name Имя симулятора (например, "iPhone 15 Pro").
     * @property state Текущее состояние устройства (например, "Booted", "Shutdown").
     */
    @Serializable
    data class Simulator(val udid: String, val name: String, val state: String)
}
