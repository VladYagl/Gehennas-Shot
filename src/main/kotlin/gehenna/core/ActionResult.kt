package gehenna.core

import gehenna.component.Position
import java.io.Serializable

typealias Sense = String

data class LogEntry(val text: String, val position: Position?, val sense: Sense): Serializable

data class ActionResult(
        val time: Long,
        val succeeded: Boolean,
        val logEntries: List<LogEntry> = emptyList(),
        val addToQueue: Boolean = true
): Serializable