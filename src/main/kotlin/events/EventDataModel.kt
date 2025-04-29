package events

import kotlinx.serialization.Serializable

/**
 * Модель события, зафиксированного в процессе тестирования.
 *
 * Используется для сериализации и хранения данных о сетевых запросах,
 * отправленных приложением во время выполнения тестов.
 *
 * Все события записываются в формате [Event] и могут быть использованы
 * для валидации отправляемых данных и воспроизведения сетевого поведения.
 *
 * @property event_time Метка времени события (например, Instant.now().toString()).
 * @property event_num Уникальный номер события в рамках одной сессии тестирования.
 * @property name Название события (например, HTTP-метод или логическое имя запроса).
 * @property data Детали события, включая тело запроса и метаинформацию [EventData].
 */
@Serializable
data class Event(
    val event_time: String,
    val event_num: Int,
    val name: String,
    val data: EventData? = null
)

/**
 * Детальная информация о сетевом запросе, связанная с событием [Event].
 *
 * Модель предназначена для хранения всей полезной нагрузки запроса,
 * включая URI запроса, IP-адрес отправителя, заголовки, параметры запроса и тело.
 *
 * @property uri Путь запроса без доменного имени (например, "/m/batch").
 * @property remoteAddress Адрес клиента, отправившего запрос (например, "192.168.1.2:53427").
 * @property headers Коллекция заголовков HTTP-запроса.
 * @property query Строка параметров запроса (query string), если есть.
 * @property body Тело запроса в формате JSON (строка).
 */
@Serializable
data class EventData(
    val uri: String,
    val remoteAddress: String,
    val headers: Map<String, List<String>>,
    val query: String? = null,
    val body: String
)
