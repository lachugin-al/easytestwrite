package unit.events

import events.Event
import events.EventData
import events.EventStorage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Модульные тесты для класса [EventStorage].
 *
 * Тестирует основную функциональность системы хранения событий:
 * - Добавление событий
 * - Отметка событий как сопоставленных
 * - Получение событий
 * - Очистка хранилища
 */
class EventStorageTest {

    @BeforeEach
    fun setUp() {
        // Очищаем хранилище перед каждым тестом
        EventStorage.clear()
    }

    @AfterEach
    fun tearDown() {
        // Очищаем хранилище после каждого теста
        EventStorage.clear()
    }

    @Test
    fun `test add events`() {
        // Создаем тестовые события
        val event1 = createTestEvent(1, "test_event_1")
        val event2 = createTestEvent(2, "test_event_2")

        // Добавляем события в хранилище
        EventStorage.addEvents(listOf(event1, event2))

        // Проверяем, что события были добавлены
        val events = EventStorage.getEvents()
        assertEquals(2, events.size)
        assertTrue(events.any { it.event_num == 1 && it.name == "test_event_1" })
        assertTrue(events.any { it.event_num == 2 && it.name == "test_event_2" })
    }

    @Test
    fun `test add duplicate events`() {
        // Создаем тестовые события с одинаковым номером события
        val event1 = createTestEvent(1, "test_event_1")
        val event2 = createTestEvent(1, "test_event_duplicate")

        // Добавляем события в хранилище
        EventStorage.addEvents(listOf(event1))
        EventStorage.addEvents(listOf(event2))

        // Проверяем, что только первое событие было добавлено
        val events = EventStorage.getEvents()
        assertEquals(1, events.size)
        assertEquals("test_event_1", events[0].name)
    }

    @Test
    fun `test mark event as matched`() {
        // Создаем и добавляем тестовые события
        val event1 = createTestEvent(1, "test_event_1")
        val event2 = createTestEvent(2, "test_event_2")
        EventStorage.addEvents(listOf(event1, event2))

        // Отмечаем событие как сопоставленное
        EventStorage.markEventAsMatched(1)

        // Проверяем, что событие отмечено как сопоставленное
        assertTrue(EventStorage.isEventAlreadyMatched(1))
        assertFalse(EventStorage.isEventAlreadyMatched(2))
    }

    @Test
    fun `test get index events`() {
        // Создаем и добавляем тестовые события
        val events = (1..5).map { createTestEvent(it, "test_event_$it") }
        EventStorage.addEvents(events)

        // Отмечаем некоторые события как сопоставленные
        EventStorage.markEventAsMatched(2)
        EventStorage.markEventAsMatched(4)

        // Получаем события начиная с индекса 0
        val indexEvents = EventStorage.getIndexEvents(0)

        // Проверяем, что возвращаются только несопоставленные события
        assertEquals(3, indexEvents.size)
        assertTrue(indexEvents.any { it.event_num == 1 })
        assertFalse(indexEvents.any { it.event_num == 2 }) // Сопоставлено, не должно быть включено
        assertTrue(indexEvents.any { it.event_num == 3 })
        assertFalse(indexEvents.any { it.event_num == 4 }) // Сопоставлено, не должно быть включено
        assertTrue(indexEvents.any { it.event_num == 5 })
    }

    @Test
    fun `test get last event`() {
        // Создаем и добавляем тестовые события
        val event1 = createTestEvent(1, "test_event_1")
        val event2 = createTestEvent(2, "test_event_2")

        // Добавляем события в хранилище
        EventStorage.addEvents(listOf(event1, event2))

        // Проверяем последнее событие
        val lastEvent = EventStorage.getLastEvent()
        assertNotNull(lastEvent)
        assertEquals(2, lastEvent?.event_num)
        assertEquals("test_event_2", lastEvent?.name)
    }

    @Test
    fun `test clear storage`() {
        // Создаем и добавляем тестовые события
        val events = (1..3).map { createTestEvent(it, "test_event_$it") }
        EventStorage.addEvents(events)

        // Отмечаем событие как сопоставленное
        EventStorage.markEventAsMatched(2)

        // Очищаем хранилище
        EventStorage.clear()

        // Проверяем, что хранилище пусто
        assertTrue(EventStorage.getEvents().isEmpty())
        assertFalse(EventStorage.isEventAlreadyMatched(2))
    }

    /**
     * Вспомогательный метод для создания тестового события.
     */
    private fun createTestEvent(eventNum: Int, eventName: String): Event {
        return Event(
            event_time = Instant.now().toString(),
            event_num = eventNum,
            name = eventName,
            data = EventData(
                uri = "/test/uri",
                remoteAddress = "127.0.0.1:8000",
                headers = mapOf("Content-Type" to listOf("application/json")),
                query = "param=value",
                body = "{\"test\": \"data\"}"
            )
        )
    }
}
