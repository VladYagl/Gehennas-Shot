package gehenna.core

import gehenna.component.Position
import gehenna.component.Senses

abstract class Action(open var time: Long = 100, open val addToQueue: Boolean = true) {
    abstract fun perform(context: Context): ActionResult

    protected val log = ArrayList<LogEntry>()
    protected fun log(text: String, position: Position?, sense: Sense = Senses.Sight::class) {
        log.add(LogEntry(text, position, sense))
    }

    protected fun end(): ActionResult = ActionResult(time, true, log, addToQueue)
    protected fun fail(): ActionResult = ActionResult(0, false, log, addToQueue)
}