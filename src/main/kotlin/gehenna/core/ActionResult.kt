package gehenna.core

import gehenna.component.Position
import gehenna.component.Senses
import java.io.Serializable
import kotlin.reflect.KClass

typealias Sense = KClass<out Senses>
typealias Animation = (() -> Unit)

interface ResultEntry {
    val position: Position?
    val sense: Sense
}

data class LogEntry(val text: String, override val position: Position?, override val sense: Sense) : Serializable, ResultEntry

data class AnimationEntry(val animation: Animation, override val position: Position?, override val sense: Sense) : Serializable, ResultEntry

data class ActionResult(
        val time: Long,
        val succeeded: Boolean,
        val entries: List<ResultEntry> = emptyList(),
        val addToQueue: Boolean = true
) : Serializable