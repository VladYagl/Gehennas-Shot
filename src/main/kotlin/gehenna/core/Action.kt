package gehenna.core

import gehenna.component.Position
import gehenna.component.Senses

abstract class Action {
    abstract val time: Long
    abstract fun perform(): ActionResult

    protected val log = ArrayList<LogEntry>()
    protected fun log(text: String, position: Position?, sense: Sense = Senses.Sight::class) {
        log.add(LogEntry(text, position, sense))
    }

    protected fun end(): ActionResult = ActionResult(time, true, log)
    protected fun fail(): ActionResult = ActionResult(0, false, log)
}