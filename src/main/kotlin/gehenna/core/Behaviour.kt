package gehenna.core

import gehenna.component.Position
import gehenna.exception.GehennaException
import gehenna.utils.random
import kotlin.math.roundToLong
import kotlin.random.asJavaRandom

abstract class Behaviour(protected open val speed: Int = normalSpeed) : ActiveComponent() {

    companion object {
        const val normalSpeed: Int = 100

        fun scaleTime(time: Long, speed: Int): Long {
            return time * normalSpeed / speed
        }

        fun randomizeTime(time: Long, speed: Int): Long {
            // TODO: 3 is a good number chosen empirically / put it somewhere else
            val drop = (random.asJavaRandom().nextGaussian() * 3 + time) * normalSpeed / speed
//            println("drop = ${drop.format(5).padEnd(10)} |" +
//                    " time = ${time.toString().padEnd(4)} |" +
//                    " speed = ${speed.toString().padEnd(5)} |" +
//                    " result = ${drop.roundToLong()}")
            return (drop).roundToLong()
        }
    }

    final override suspend fun action(): Action {
        val action = behave()
//      action.time = scaleTime(action.time, speed)
        action.time = randomizeTime(action.time, speed)
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
