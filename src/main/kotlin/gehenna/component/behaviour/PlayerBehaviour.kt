package gehenna.component.behaviour

import gehenna.component.Senses
import gehenna.core.Action
import gehenna.core.Entity
import kotlinx.coroutines.channels.Channel

data class PlayerBehaviour(override val entity: Entity, public override val speed: Int = 100) : CharacterBehaviour() {
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
}