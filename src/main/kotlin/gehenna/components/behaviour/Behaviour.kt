package gehenna.components.behaviour

import gehenna.actions.Action
import gehenna.actions.ActionResult
import gehenna.components.WaitTime

abstract class Behaviour : WaitTime() {
    abstract val action: Action
    var lastResult: ActionResult? = null
}