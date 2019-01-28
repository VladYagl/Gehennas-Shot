package gehenna.component

import gehenna.action.Destroy
import gehenna.core.*

abstract class ActiveComponent : Component() {
    open var waitTime: Long = 0L
    abstract suspend fun action(): Action
    var lastResult: ActionResult? = null

    init {
        //todo: get ActionQueue from somewhere i dont wanna this static bullshit
        subscribe<Entity.Add> { ActionQueue.add(this) }
        subscribe<Entity.Remove> { ActionQueue.remove(this) }
    }
}

abstract class Effect : ActiveComponent() {
    abstract var duration: Long
}

data class DestroyTimer(override val entity: Entity, override var waitTime: Long = 1000) : Effect() {
    override var duration
        get() = 1L
        set(value) {}

    override suspend fun action(): Action = Destroy(entity)
}

open class RepeatAction<T : Action>(
    override val entity: Entity,
    private var count: Int,
    private var delay: Long = 100,
    private val actionFactory: () -> T
) : Effect() {
    override var waitTime = 1L
    override var duration
        get() = if (count > 0) 1L else 0L
        set(_) {}

    override suspend fun action(): T {
        count--
        return actionFactory()
    }
}

data class SequenceOfActions(
    override val entity: Entity,
    private val actions: Iterable<Action>,
    private var delay: Long = 100
) : Effect() {
    override var waitTime = 1L
    override var duration
        get() = if (iterator.hasNext()) 1L else 0L
        set(_) {}

    private val iterator = actions.iterator()
    override suspend fun action() = iterator.next()
}

