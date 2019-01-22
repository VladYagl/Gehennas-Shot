package gehenna.component.behaviour

import gehenna.core.Entity
import gehenna.action.Move
import gehenna.component.ActiveComponent
import gehenna.utils.Dir
import gehenna.utils.random

abstract class Behaviour : ActiveComponent()

abstract class PredictableBehaviour : Behaviour() {
    abstract fun copy(entity: Entity): Behaviour
}

data class RandomBehaviour(override val entity: Entity) : Behaviour() {
    override suspend fun action() = Move(entity, Dir.random(random))
}
