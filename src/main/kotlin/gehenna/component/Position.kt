package gehenna.component

import gehenna.core.Component
import gehenna.core.Entity
import gehenna.level.Level
import gehenna.utils.Point

data class Position(
    override val x: Int,
    override val y: Int,
    val level: Level,
    override val entity: Entity
) : Component(), Point {

    fun move(x: Int, y: Int) {
        level.move(entity, x, y)
    }

    fun spawnHere(entity: Entity) {
        level.spawn(entity, x, y)
    }

    fun update() = level.update(x, y)

    val neighbors: List<Entity> get() = level[x, y].filter { it != entity }

    init {
        subscribe<Entity.Add> { level.spawn(this) }
        subscribe<Entity.Remove> { level.remove(this) }
    }

    fun findPath(toX: Int, toY: Int): List<Point>? {
        return level.findPath(x, y, toX, toY)
    }
}