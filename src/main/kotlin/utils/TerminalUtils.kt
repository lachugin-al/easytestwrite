package utils


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
}
