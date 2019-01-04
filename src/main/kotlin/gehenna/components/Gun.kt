package gehenna.components

import gehenna.Entity
import gehenna.actions.Action
import gehenna.actions.Shoot
import gehenna.utils.Point

abstract class Gun : Component() {
    abstract fun fire(actor: Entity, dir: Point): Action
}

data class Pistol(override val entity: Entity, private val bullet: String) : Gun() {
    //TODO: get actor from inventory or equipment
    override fun fire(actor: Entity, dir: Point): Action = Shoot(actor, dir, entity.factory.newEntity(bullet))
}

