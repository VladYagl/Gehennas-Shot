package gehenna.core

import gehenna.component.Position
import gehenna.exception.GehennaException

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
