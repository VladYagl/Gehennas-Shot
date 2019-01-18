package gehenna.component

import gehenna.action.Destroy
import gehenna.core.*

abstract class ActiveComponent : Component() {
    open var waitTime: Long = 0L
    abstract suspend fun action(): Action
    var lastResult: ActionResult? = null

    init {
        subscribe<Entity.Add> { ActionQueue.add(this) }
        subscribe<Entity.Remove> { ActionQueue.remove(this) }
    }
}

abstract class Effect : ActiveComponent() {
    abstract var duration: Long
}

data class DestroyTimer(override val entity: Entity, override var waitTime: Long = 1000) : Effect() {
    override var duration: Long = waitTime
    override suspend fun action(): Action = Destroy(entity)
}

open class RepeatAction<T : Action>(
    override val entity: Entity,
    private var count: Int,
    private var delay: Long = 100,
    private val actionFactory: () -> T
) : Effect() {
    override var waitTime = 1L
    override var duration = 1L
    override suspend fun action(): T {
        if (--count > 0) duration += delay // TODO <- get action waitTime here or some thing
        return actionFactory()
    }
}

data class SequenceOfActions(
    override val entity: Entity,
    private val actions: Iterable<Action>,
    private var delay: Long = 100
) : Effect() {
    override var waitTime = 1L
    override var duration: Long = waitTime
    private val iterator = actions.iterator()
    override suspend fun action(): Action {
        val value = iterator.next()
        if (iterator.hasNext()) duration += waitTime
        return value
    }
}

