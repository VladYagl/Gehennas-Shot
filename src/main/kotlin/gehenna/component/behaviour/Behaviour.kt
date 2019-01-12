package gehenna.component.behaviour

import gehenna.core.Action
import gehenna.core.ActionResult
import gehenna.component.WaitTime

abstract class Behaviour : WaitTime() {
    abstract suspend fun action(): Action
    var lastResult: ActionResult? = null
}