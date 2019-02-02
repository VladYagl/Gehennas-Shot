package gehenna.component.behaviour

import gehenna.action.Move
import gehenna.component.ActiveComponent
import gehenna.component.Position
import gehenna.core.Action
import gehenna.core.Entity
import gehenna.core.Faction
import gehenna.core.SoloFaction
import gehenna.utils.Dir
import gehenna.utils.random

abstract class Behaviour(protected open val speed: Int = 100) : ActiveComponent() {
    protected fun scaleTime(time: Long, speed: Int): Long {
        return time * 100 / speed
    }

    final override suspend fun action(): Action {
        val action = behave()
        action.time = scaleTime(action.time, speed)
        return action
    }

    protected abstract suspend fun behave(): Action
}

abstract class CharacterBehaviour : Behaviour() {
    open val faction: Faction = SoloFaction
}

abstract class PredictableBehaviour : Behaviour() {
    abstract fun copy(entity: Entity): Behaviour

    fun predict(pos: Position, dir: Dir): Action {
        val action = predictImpl(pos, dir)
        action.time = scaleTime(action.time, speed)
        return action
    }

    protected abstract fun predictImpl(pos: Position, dir: Dir): Action
}

data class RandomBehaviour(override val entity: Entity) : Behaviour() {
    override suspend fun behave() = Move(entity, Dir.random(random))
}
