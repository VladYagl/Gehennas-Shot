package gehenna.core

import gehenna.component.Glyph
import gehenna.component.Logger
import gehenna.component.Position
import gehenna.component.Senses
import gehenna.utils.Point
import gehenna.utils.prepareMessage
import java.io.Serializable

abstract class Action(open var time: Long = oneTurn, open val addToQueue: Boolean = true) : Serializable {

    companion object {
        const val oneTurn: Long = 100
    }

    abstract fun perform(context: Context): ActionResult

    protected val log = ArrayList<LogEntry>()
    private fun log(text: String, position: Position?, sense: Sense = Senses.Sight::class.simpleName!!) {
        log.add(LogEntry(text, position, sense))
    }

    fun logFor(actor: Entity, message: String, args: Map<String, String> = emptyMap()) {
        actor<Logger>()?.add(message.prepareMessage(true, actor, args))
                ?: log(message.prepareMessage(false, actor, args), actor())
    }

    protected fun end(): ActionResult = ActionResult(time, true, log, addToQueue)
    protected fun fail(): ActionResult = ActionResult(0, false, log, addToQueue)
}

abstract class PredictableAction<T>(time: Long = oneTurn, addToQueue: Boolean = true) : Action(time, addToQueue) {
    open fun predict(pos: Position, state: T, glyph: Glyph): Triple<Point, T, Glyph> = Triple(pos, state, glyph)
}