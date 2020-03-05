package gehenna.component

import gehenna.core.Component
import gehenna.core.Entity
import gehenna.level.FovLevel
import gehenna.utils.Point

sealed class Senses : Component() {
    abstract fun visitFov(visitor: (Entity, Point) -> Unit)
    abstract fun isVisible(point: Point): Boolean

    data class Sight(override val entity: Entity, val range: Int) : Senses() {
        private var seen = HashMap<Entity, Long>()
        private var count = 0L
        @Transient
        private var fov: FovLevel.FovBoard? = null

        override fun visitFov(visitor: (Entity, Point) -> Unit) {
            val pos = entity<Position>()
            fov = pos?.level?.visitFov(pos, range) { target, point ->
                visitor(target, point)
                if (seen[target] != count) entity.emit(Saw(target))
                seen[target] = count + 1
            }
            count++
        }

        override fun isVisible(point: Point) = fov?.isVisible(point) ?: false

        data class Saw(val entity: Entity) : Entity.Event
    }

    data class Smell(override val entity: Entity, val range: Int) : Senses() {
        override fun isVisible(point: Point): Boolean = (entity.one<Position>() - point).max <= range

        override fun visitFov(visitor: (Entity, Point) -> Unit) {
            entity<Position>()?.let { pos ->
                for (point in pos.level.size.range) {
                    if ((point - pos).max <= range) {
                        pos.level[point].forEach { entity -> visitor(entity, point) }
                    }
                }
            }
        }
    }

    data class TrueSight(override val entity: Entity) : Senses() {
        override fun isVisible(point: Point): Boolean = true

        override fun visitFov(visitor: (Entity, Point) -> Unit) {
            entity<Position>()?.let { pos ->
                for (point in pos.level.size.range) {
                    pos.level[point].forEach { entity -> visitor(entity, point) }
                }
            }
        }
    }

    data class Hearing(override val entity: Entity) : Senses() {
        //todo: is visible should return true in radius, so you can add stuff like hearing gun shots
        override fun isVisible(point: Point): Boolean = false

        override fun visitFov(visitor: (Entity, Point) -> Unit) {} // TODO
    }
}