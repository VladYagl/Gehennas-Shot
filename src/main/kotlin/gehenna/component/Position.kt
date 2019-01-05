package gehenna.component

import gehenna.core.Component
import gehenna.core.Entity
import gehenna.level.Level
import gehenna.utils.Point
import gehenna.utils.x
import gehenna.utils.y

data class Position(
        override val entity: Entity,
        val x: Int,
        val y: Int,
        val level: Level
) : Component() {
    val point: Point
        get() {
            return x to y
        }

    operator fun plus(dir: Point): Point {
        return x + dir.x to y + dir.y
    }

    fun move(x: Int, y: Int) {
        level.move(entity, x, y)
    }

    fun spawnHere(entity: Entity) {
        level.spawn(entity, x, y)
    }

    val neighbors: List<Entity> get() = level[x, y].filter { it != entity }

    override fun onEvent(event: Entity.Event) {
        when (event) {
            is Entity.Add -> level.spawn(this)
            is Entity.Remove -> level.remove(this)
        }
    }

    fun findPath(toX: Int, toY: Int): List<Point>? {
        return level.findPath(x, y, toX, toY)
    }
}