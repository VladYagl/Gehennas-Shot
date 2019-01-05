package gehenna.component

import gehenna.core.Entity
import gehenna.core.Action
import gehenna.action.ApplyEffect
import gehenna.action.Shoot
import gehenna.core.Component
import gehenna.utils.Point

abstract class Gun : Component() {
    abstract fun fire(actor: Entity, dir: Point): Action?
}

data class Pistol(override val entity: Entity, private val bullet: String) : Gun() {
    //TODO: get actor from inventory or equipment
    override fun fire(actor: Entity, dir: Point): Action = Shoot(actor, dir, entity.factory.new(bullet))
}

data class Rifle(override val entity: Entity, private val bullet: String) : Gun() {
    override fun fire(actor: Entity, dir: Point): Action? =
            if (!entity.has(BurstFire::class)) {
                ApplyEffect(actor, BurstFire(actor, bullet, actor, dir,1))
            } else null
}

data class BurstFire(override val entity: Entity, val bullet: String, val actor: Entity, val dir: Point, override var time: Long) : Effect() {
    override var duration: Long = 500
    override val action: Action get() = Shoot(actor, dir, entity.factory.new(bullet))
}

