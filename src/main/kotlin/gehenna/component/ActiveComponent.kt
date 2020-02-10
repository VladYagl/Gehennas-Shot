package gehenna.component

import gehenna.action.Destroy
import gehenna.core.*
import gehenna.core.Action.Companion.oneTurn
import kotlin.reflect.jvm.internal.impl.load.java.typeEnhancement.TypeEnhancementInfo

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
    open var duration: Long = 0
    open val endless: Boolean = false
}

data class PassiveHeal(
        override val entity: Entity,
        override var waitTime: Long = 1L,
        val oneTick: Long = 500,
        val amount: Int = 1
) : Effect() {
    override val endless: Boolean = true

    override suspend fun action() = SimpleAction(oneTick) {
        entity<Health>()?.heal(amount)
    }
}

data class DestroyTimer(override val entity: Entity, override var waitTime: Long = oneTurn * 10) : Effect() {
    override var duration
        get() = 1L
        set(value) {}

    override suspend fun action(): Action = Destroy(entity)
}

open class RepeatAction<T : Action>(
        override val entity: Entity,
        private var count: Int,
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
        private val actions: Iterable<Action>
) : Effect() {
    override var waitTime = 1L
    override var duration
        get() = if (iterator.hasNext()) 1L else 0L
        set(_) {}

    private val iterator = actions.iterator()
    override suspend fun action() = iterator.next()
}

