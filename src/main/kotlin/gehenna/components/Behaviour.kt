package gehenna.components

import gehenna.Action
import gehenna.ActionResult
import gehenna.Entity
import gehenna.Move
import gehenna.utils.random
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking

abstract class Behaviour : WaitTime() {
    abstract val action: Action
    var lastResult: ActionResult? = null
}

abstract class PredictableBehaviour : Behaviour() {
    abstract fun copy(entity: Entity): Behaviour
}

data class ThinkUntilSet(override val entity: Entity) : Behaviour() {
    private val channel = Channel<Action>(Channel.CONFLATED)
    override var action: Action
        get() = runBlocking { channel.receive() }
        set(value) = runBlocking { channel.send(value) }
}

data class RandomBehaviour(override val entity: Entity) : Behaviour() {
    override val action: Action get() = Move(entity, (random.nextInt(3) - 1) to (random.nextInt(3) - 1))
}
