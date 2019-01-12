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
    private val delay: Long,
    private val burst: Boolean = false,
    private val burstCount: Int = 5,
    private val time: Long = 100
) : Component() {
    fun action(actor: Entity, dir: Point): Action = Shoot(actor[Position::class]!!, dir, bullet, delay, time)

    fun fire(actor: Entity, dir: Point): Action? {
        return if (burst) {
            if (!actor.has(RepeatAction::class)) { //TODO
                ApplyEffect(actor, RepeatAction(actor, burstCount, time) { action(actor, dir) })
            } else null
        } else {
            action(actor, dir)
        }
    }
}
