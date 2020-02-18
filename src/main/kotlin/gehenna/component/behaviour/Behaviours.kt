package gehenna.component.behaviour

import gehenna.action.Move
import gehenna.action.Think
import gehenna.component.ActiveComponent
import gehenna.component.Position
import gehenna.core.*
import gehenna.core.Action.Companion.oneTurn
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

abstract class PredictableBehaviour<T> : Behaviour() {
    abstract val state: T

    fun predict(pos: Position, state: T): PredictableAction<in T> {
        val action = predictImpl(pos, state)
        action.time = scaleTime(action.time, speed)
        return action
    }

    protected abstract fun predictImpl(pos: Position, state: T): PredictableAction<in T>

    override suspend fun behave(): Action {
        if (lastResult?.succeeded == false) {
            throw GehennaException("Predictable behaviour failed action: $lastResult")
        }
        return predictImpl(entity.one(), state)
    }
}

data class RandomBehaviour(override val entity: Entity) : Behaviour() {
    override suspend fun behave() = Move(entity, Dir.random(random))
}

data class NoBehaviour(override val entity: Entity) : CharacterBehaviour() {
    override suspend fun behave() = Think(oneTurn * 100)
}
