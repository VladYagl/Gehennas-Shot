package gehenna.component

import gehenna.core.Entity
import gehenna.core.Action
import gehenna.action.Destroy
import gehenna.utils.Point

abstract class Effect : WaitTime() {
    abstract var duration: Long
    abstract val action: Action
}

data class DestroyTimer(override val entity: Entity, override var time: Long = 1000) : Effect() {
    override var duration: Long = time
    override val action: Action = Destroy(entity)
}

data class RepeatAction<T : Action>(
    override val entity: Entity,
    private var count: Int,
    private var delay: Long = 100,
    private val actionFactory: () -> T
) : Effect() {
    override var time = 1L
    override var duration = 1L
    override val action: T
        get() {
            if (--count > 0) duration += delay // TODO <- get action time here or some thing
            return actionFactory()
        }
}

data class SequenceOfActions(
    override val entity: Entity,
    private val actions: Iterable<Action>
) : Effect() {
    override var duration: Long = time
    private val iterator = actions.iterator()
    override val action: Action
        get() {
            val value = iterator.next()
            if (iterator.hasNext()) duration += time
            return value
        }
}

