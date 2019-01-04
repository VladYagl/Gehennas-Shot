package gehenna.components

import gehenna.actions.Action

abstract class Effect : WaitTime() {
    abstract var duration: Long
    abstract val action: Action
}
