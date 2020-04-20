package gehenna.component

import gehenna.core.Component
import gehenna.core.Entity
import gehenna.level.Level
import gehenna.utils.Dir
import gehenna.utils.Point

data class Position(
        override val x: Int,
        override val y: Int,
        val level: Level,
        override val entity: Entity,
        private val lastPoint: Point? = null
) : Component(), Point {

    data class Spawn(val pos: Position) : Entity.Event
    data class Despawn(val pos: Position) : Entity.Event

    constructor(point: Point, level: Level, entity: Entity, lastPoint: Point? = null) : this(point.x, point.y, level, entity, lastPoint)

    val lastDir: Dir? get() = lastPoint?.let { (this - it).dir }

    fun move(point: Point) {
        level.move(entity, point)
    }

    fun spawnHere(entity: Entity) {
        level.spawn(entity, this)
    }

    fun update() = level.update(this)

    val neighbors: List<Entity> get() = level[this].filter { it != entity }

    init {
        subscribe<Entity.Add> {
            level.spawn(this)
            entity.emit(Spawn(this))
        }
        subscribe<Entity.Remove> {
            entity.emit(Despawn(this))
            level.remove(this)
        }
    }

    fun findPath(to: Point): List<Point>? {
        return level.findPath(this, to)
    }
}