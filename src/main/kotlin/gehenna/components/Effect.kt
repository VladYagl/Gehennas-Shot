package gehenna.components

import gehenna.Action
import gehenna.Entity
import gehenna.Shoot
import gehenna.utils.Point

abstract class Effect : WaitTime() {
    abstract var duration: Long
    abstract val action: Action
}

data class RunAndGun(
        override val entity: Entity,
        private val dir: Point,
        private val gun: Gun,
        override var duration: Long,
        override var time: Long = 99
) : Effect() {
    override val action: Action = Shoot(entity, dir, gun)
}
