package gehenna.component

import gehenna.action.ApplyEffect
import gehenna.action.Shoot
import gehenna.core.Action
import gehenna.core.Component
import gehenna.core.Entity
import gehenna.utils.Point

data class Gun(
    override val entity: Entity,
    private val bullet: String,
    private val damage: Int,
    private val delay: Long,
    private val burst: Boolean = false,
    private val burstCount: Int = 5,
    private val time: Long = 100
) : Component() {
    fun action(actor: Entity, dir: Point) = Shoot(actor()!!, dir, bullet, damage, delay, time)

    fun fire(actor: Entity, dir: Point): Action? {
        return if (burst) {
            if (!actor.has<BurstFire>()) {
                ApplyEffect(actor, BurstFire(actor, dir, this))
            } else null
        } else {
            action(actor, dir)
        }
    }

    data class BurstFire(private val actor: Entity, private val dir: Point, private val gun: Gun) :
        RepeatAction<Shoot>(actor, gun.burstCount, gun.time, { gun.action(actor, dir) })
}
