package gehenna.core

import gehenna.component.Logger
import gehenna.component.Position
import gehenna.component.Senses
import gehenna.utils.prepareMessage
import java.io.Serializable

abstract class Action(open var time: Long = 100, open val addToQueue: Boolean = true) : Serializable {
    abstract fun perform(context: Context): ActionResult

    protected val log = ArrayList<LogEntry>()
    protected fun log(text: String, position: Position?, sense: Sense = Senses.Sight::class.simpleName!!) {
        log.add(LogEntry(text, position, sense))
    }

    protected fun logFor(actor: Entity, message: String, args: Map<String, String> = emptyMap()) {
        actor<Logger>()?.add(message.prepareMessage(true, actor, args))
                ?: log(message.prepareMessage(false, actor, args), actor())
    }

    protected fun end(): ActionResult = ActionResult(time, true, log, addToQueue)
    protected fun fail(): ActionResult = ActionResult(0, false, log, addToQueue)
}