package gehenna.component.behaviour

import gehenna.component.Senses
import gehenna.core.Action
import gehenna.core.Entity
import gehenna.utils.setVal
import kotlinx.coroutines.channels.Channel
import java.io.ObjectInputStream

data class PlayerBehaviour(override val entity: Entity, public override val speed: Int = 100) : CharacterBehaviour() {
    @Transient
    private val channel = Channel<Action>(Channel.CONFLATED)
    private var repeatAction: Action? = null

    override suspend fun behave(): Action {
        if (lastResult?.succeeded == false) repeatAction = null
        return repeatAction ?: channel.receive()
    }

    fun set(action: Action) {
        repeatAction = null
        channel.offer(action)
    }

    fun repeat(action: Action) {
        channel.offer(action)
        repeatAction = action
    }

    init {
        subscribe<Senses.Sight.Saw> { if (it.entity.any<Behaviour>() != null) repeatAction = null }
    }

    private fun readObject(inputStream: ObjectInputStream) {
        inputStream.defaultReadObject()
        this.setVal("channel", Channel<Action>(Channel.CONFLATED))
    }
}