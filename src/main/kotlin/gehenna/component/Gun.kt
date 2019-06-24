package gehenna.component

import gehenna.action.ApplyEffect
import gehenna.action.Shoot
import gehenna.core.Action
import gehenna.core.Component
import gehenna.core.Entity
import gehenna.utils.Dice
import gehenna.utils.Dir

data class Gun(
        override val entity: Entity,
        private val bullet: String,
        private val damage: Dice,
        private val speed: Int,
        private val delay: Long,
        private val burst: Boolean = false,
        private val burstCount: Int = 5,
        private val time: Long = 100
) : Component() {
    private fun action(actor: Entity, dir: Dir) = Shoot(actor.one(), dir, bullet, damage, delay, speed, time)

    private data class BurstFire(private val actor: Entity, private val dir: Dir, private val gun: Gun) :
            RepeatAction<Shoot>(actor, gun.burstCount, gun.time, { gun.action(actor, dir) }) {
        override fun toString(): String {
            return "burst fire $dir"
        }
    }

    fun fire(actor: Entity, dir: Dir): Action? {
        return if (burst) {
            if (!actor.has<BurstFire>()) {
                ApplyEffect(actor, BurstFire(actor, dir, this))
            } else null
        } else {
            action(actor, dir)
        }
    }
}
