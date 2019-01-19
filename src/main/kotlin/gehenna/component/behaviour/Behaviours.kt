package gehenna.component.behaviour

import gehenna.core.Entity
import gehenna.core.Action
import gehenna.action.Move
import gehenna.component.ActiveComponent
import gehenna.utils.at
import gehenna.utils.Dir
import gehenna.utils.random
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

abstract class Behaviour : ActiveComponent()

abstract class PredictableBehaviour : Behaviour() {
    abstract fun copy(entity: Entity): Behaviour
}

data class ThinkUntilSet(override val entity: Entity) : Behaviour() {
    private val channel = Channel<Action>(Channel.CONFLATED)
    override suspend fun action() = channel.receive()
    var action: Action
        get() = runBlocking { channel.receive() }
        set(value) {
            channel.offer(value)
        }
//    override var action: Action = Think()
//        get() {
//            val res = field
//            field = Think()
//            return res
//        }
}

data class RandomBehaviour(override val entity: Entity) : Behaviour() {
    override suspend fun action() = Move(entity, Dir.random(random))
}
