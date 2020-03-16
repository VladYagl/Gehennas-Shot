package gehenna.core

import gehenna.utils.*
import gehenna.component.*
import gehenna.ui.UIContext
import gehenna.utils.Point
import gehenna.utils.prepareMessage
import java.io.Serializable

abstract class Action(open var time: Long = oneTurn, open val addToQueue: Boolean = true) : Serializable {

    companion object {
        const val oneTurn: Long = 100
    }

    abstract fun perform(context: UIContext): ActionResult

    protected val results = ArrayList<ResultEntry>()
    private fun log(text: String, position: Position?, sense: Sense = Senses.Sight::class) {
        results.add(LogEntry(text, position, sense))
    }

    fun logFor(actor: Entity, message: String, args: Map<String, String> = emptyMap()) {
        actor<Logger>()?.add(message.prepareMessage(true, actor, args))
                ?: log(message.prepareMessage(false, actor, args), actor())
    }

    fun animate(pos: Position, sense: Sense = Senses.Sight::class, animation: Animation) {
        results.add(AnimationEntry(animation, pos, sense))
    }

    protected fun end(): ActionResult = ActionResult(time, true, results, addToQueue)

    protected fun fail(): ActionResult = ActionResult(0, false, results, addToQueue)
}

class SimpleAction(time: Long = oneTurn, addToQueue: Boolean = true, val func: (context: UIContext) -> Unit) : Action(time, addToQueue) {
    override fun perform(context: UIContext): ActionResult {
        func(context)
        return end()
    }
}

class SimpleReturnAction(
        time: Long = oneTurn,
        addToQueue: Boolean = true,
        val func: (context: UIContext) -> ActionResult
) : Action(time, addToQueue) {
    override fun perform(context: UIContext): ActionResult {
        return func(context)
    }
}

abstract class PredictableAction<T>(time: Long = oneTurn, addToQueue: Boolean = true) : Action(time, addToQueue) {
    open fun predict(pos: Position, state: T, glyph: Glyph): Triple<Point, T, Glyph> = pos to state to glyph
}