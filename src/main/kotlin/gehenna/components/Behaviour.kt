package gehenna.components

import gehenna.*
import gehenna.utils.random

abstract class Behaviour : WaitTime() {
    abstract val action: Action
    var lastResult: ActionResult? = null
}

abstract class PredictableBehaviour : Behaviour() {
    abstract fun copy(entity: Entity): Behaviour
}

data class ThinkUntilSet(override val entity: Entity) : Behaviour() {
    override var action: Action = Think()
        get() {
            val res = field
            field = Think()
            return res
        }
}

data class RandomBehaviour(override val entity: Entity) : Behaviour() {
    override val action: Action get() = Move(entity, (random.nextInt(3) - 1) to (random.nextInt(3) - 1))
}
