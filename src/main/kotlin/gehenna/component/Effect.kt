package gehenna.component

import gehenna.core.Entity
import gehenna.core.Action
import gehenna.action.Destroy
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
        override var time: Long = 1
) : Effect() {
    override val action: Action = gun.fire(entity, dir)!!
}

data class DestroyTimer(override val entity: Entity, override var time: Long = 1000) : Effect() {
    override var duration: Long = time
    override val action: Action = Destroy(entity)
}
