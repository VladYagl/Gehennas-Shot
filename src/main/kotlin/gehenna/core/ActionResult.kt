package gehenna.core

import gehenna.component.Position
import gehenna.component.Senses
import kotlin.reflect.KClass

typealias Sense = KClass<out Senses>

data class LogEntry(val text: String, val position: Position?, val sense: Sense)

data class ActionResult(val time: Long, val succeeded: Boolean, val logEntries: List<LogEntry> = emptyList())