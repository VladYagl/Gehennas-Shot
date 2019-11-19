package gehenna.component.behaviour

import gehenna.action.Move
import gehenna.component.ActiveComponent
import gehenna.component.Position
import gehenna.core.*
import gehenna.exceptions.GehennaException
import gehenna.utils.Dir
import gehenna.utils.random

abstract class Behaviour(protected open val speed: Int = normalSpeed) : ActiveComponent() {

    companion object {
        const val normalSpeed: Int = 100

        fun scaleTime(time: Long, speed: Int): Long {
            return time * normalSpeed / speed
        }
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
    open val dir: Dir = Dir.zero

    fun predict(pos: Position, dir: Dir): PredictableAction {
        val action = predictImpl(pos, dir)
        action.time = scaleTime(action.time, speed)
        return action
    }

    protected abstract fun predictImpl(pos: Position, dir: Dir): PredictableAction

    override suspend fun behave(): Action {
        if (lastResult?.succeeded == false) {
            throw GehennaException("Predictable behaviour failed action: $lastResult")
        }
        return predictImpl(entity.one(), dir)
    }
}

data class RandomBehaviour(override val entity: Entity) : Behaviour() {
    override suspend fun behave() = Move(entity, Dir.random(random))
}
