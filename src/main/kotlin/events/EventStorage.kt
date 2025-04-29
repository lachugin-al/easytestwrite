package events

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Хранилище событий, зафиксированных в процессе тестирования.
 *
 * Служит для централизованного сохранения всех полученных [Event],
 * а также для управления их состоянием (обработано/не обработано).
 *
 * Используется для поиска событий по различным критериям в рамках тестовых проверок.
 */
object EventStorage {
    private val logger: Logger = LoggerFactory.getLogger(EventStorage::class.java)

    /** Список всех зафиксированных событий. */
    private val events = mutableListOf<Event>()

    /** Набор номеров событий, которые уже были обработаны в проверках. */
    private val matchedEvents = mutableSetOf<Int>()

    /**
     * Добавляет список новых событий в хранилище.
     *
     * Перед добавлением выполняется проверка на уникальность номера события ([event_num]).
     * Повторяющиеся события игнорируются.
     *
     * @param newEvents Список новых событий для добавления.
     */
    fun addEvents(newEvents: List<Event>) {
        newEvents.forEach { event ->
            if (!eventExists(event.event_num)) {
                events.add(event)
                logger.info("Сохранено событие: ${event.name}, Номер: ${event.event_num}, Время: ${event.event_time}, Данные: ${event.data}")
            }
        }
    }

    /**
     * Проверяет наличие события в хранилище по его номеру.
     *
     * @param eventNumber Номер события.
     * @return `true`, если событие уже существует, иначе `false`.
     */
    private fun eventExists(eventNumber: Int): Boolean {
        return events.any { it.event_num == eventNumber }
    }

    /**
     * Отмечает событие как обработанное (matched) по его номеру.
     *
     * @param eventNum Номер события для пометки.
     */
    fun markEventAsMatched(eventNum: Int) {
        matchedEvents.add(eventNum)
    }

    /**
     * Проверяет, было ли событие уже обработано.
     *
     * @param eventNum Номер события.
     * @return `true`, если событие уже отмечено как обработанное, иначе `false`.
     */
    fun isEventAlreadyMatched(eventNum: Int): Boolean {
        return matchedEvents.contains(eventNum)
    }

    /**
     * Получает список событий, начиная с указанного индекса.
     *
     * Исключаются уже обработанные события.
     *
     * @param index Индекс, начиная с которого нужно получить события.
     * @return Список новых событий или пустой список, если индекс за пределами текущего размера хранилища.
     */
    fun getIndexEvents(index: Int): List<Event> {
        return if (index < events.size) {
            events.subList(index, events.size).filterNot { event ->
                isEventAlreadyMatched(event.event_num)
            }
        } else {
            emptyList()
        }
    }

    /**
     * Возвращает все зафиксированные события.
     *
     * @return Копия списка всех событий.
     */
    fun getEvents(): List<Event> {
        return events.toList()
    }

    /**
     * Возвращает последнее добавленное событие.
     *
     * @return Последнее событие или `null`, если хранилище пусто.
     */
    fun getLastEvent(): Event? {
        return events.lastOrNull()
    }

    /**
     * Очищает хранилище событий и сбрасывает список обработанных событий.
     */
    fun clear() {
        events.clear()
        matchedEvents.clear()
    }
}