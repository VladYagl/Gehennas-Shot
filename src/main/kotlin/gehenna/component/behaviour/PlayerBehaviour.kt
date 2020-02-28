package gehenna.component.behaviour

import gehenna.action.Move
import gehenna.component.Position
import gehenna.component.Senses
import gehenna.core.Action
import gehenna.core.Behaviour
import gehenna.core.Entity
import gehenna.utils.Dir
import gehenna.utils.setVal
import kotlinx.coroutines.channels.Channel
import java.io.ObjectInputStream

data class PlayerBehaviour(override val entity: Entity, public override val speed: Int = 100) : CharacterBehaviour() {

    private interface Automation {
        fun nextAction(): Action?
    }

    private inner class Walk(val dir: Dir) : Automation {

        private val front: String
            get() {
                val position = entity.one<Position>()
                return listOf(dir, dir.turnLeft, dir.turnRight)
                        .map { position.level[position + it] }
                        .flatten()
                        .map(Entity::name)
                        .joinToString()
            }

        private val initFront: String = front

        override fun nextAction(): Action? {
            return if (front == initFront) {
                Move(entity, dir)
            } else {
                null
            }
        }
    }

    @Transient
    private val channel = Channel<Action>(Channel.CONFLATED)
    @Transient
    private var automation: Automation? = null

    override suspend fun behave(): Action {
        if (lastResult?.succeeded == false)
            automation = null
        return automation?.nextAction() ?: channel.receive()
    }

    fun set(action: Action) {
        automation = null
        channel.offer(action)
    }

    private fun automate(automation: Automation) {
        automation.nextAction()?.let { action ->
            lastResult = null
            this.automation = automation
            channel.offer(action)
        }
    }

    fun repeat(action: Action) {
        automate(object : Automation {
            override fun nextAction(): Action {
                return action
            }
        })
    }

    fun walk(dir: Dir) = automate(Walk(dir))

    fun cancel() {
        automation = null
    }

    init {
        subscribe<Senses.Sight.Saw> { if (it.entity.any<Behaviour>() != null) automation = null }
    }

    private fun readObject(inputStream: ObjectInputStream) {
        inputStream.defaultReadObject()
        this.setVal("channel", Channel<Action>(Channel.CONFLATED))
    }
}