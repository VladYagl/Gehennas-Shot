package gehenna.component.behaviour

import gehenna.core.Entity
import gehenna.core.Action
import gehenna.action.Move
import gehenna.utils.random
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking

abstract class PredictableBehaviour : Behaviour() {
    abstract fun copy(entity: Entity): Behaviour
}

data class ThinkUntilSet(override val entity: Entity) : Behaviour() {
        private val channel = Channel<Action>(Channel.CONFLATED)
    override var action: Action
        get() = runBlocking { channel.receive() }
        set(value) = runBlocking { channel.send(value) }
//    override var action: Action = Think()
//        get() {
//            val res = field
//            field = Think()
//            return res
//        }
}

data class RandomBehaviour(override val entity: Entity) : Behaviour() {
    override val action: Action get() = Move(entity, (random.nextInt(3) - 1) to (random.nextInt(3) - 1))
}
