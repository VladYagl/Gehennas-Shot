package gehenna.component

import gehenna.action.ApplyEffect
import gehenna.action.Shoot
import gehenna.core.Action
import gehenna.core.Component
import gehenna.core.Entity
import gehenna.core.SimpleAction
import gehenna.utils.Dice
import gehenna.utils.LineDir
import kotlin.math.max

data class Gun(
        override val entity: Entity,
        val bullet: String,
        val damage: Dice,
        val speed: Int,
        val delay: Long,
        val minSpread: Double = 0.0,
        val maxSpread: Double = 0.0,
        val shootSpread: Double = 0.0,
        val spreadReduce: Double = 0.0,
        val spreadReduceTick: Long = 10,
        private val burstCount: Int = 1,
        private val time: Long = 100
) : Component() {

    private val spreadReducer = object : Effect() {
        override val endless = true
        override val entity = this@Gun.entity

        override suspend fun action() = SimpleAction(spreadReduceTick) {
            spread = max(minSpread, spread - spreadReduce)
        }
    }

    override val children: List<Component> = listOf(spreadReducer)
    var spread = minSpread


    private fun action(actor: Entity, dir: LineDir, spread: Double) =
            Shoot(actor.one(), dir, this, time)

    data class BurstFire(private val actor: Entity, private val dir: LineDir, val gun: Gun, val spread: Double) :
            RepeatAction<Shoot>(actor, gun.burstCount, { gun.action(actor, dir, spread) }) {
        override fun toString(): String {
            return "burst fire $dir"
        }
    }

    fun fire(actor: Entity, dir: LineDir): Action? {
        return if (!actor.has<BurstFire>()) {
            ApplyEffect(actor, BurstFire(actor, dir, this, spread), time)
        } else null
    }
}
