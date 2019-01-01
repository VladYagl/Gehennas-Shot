package gehenna.components

import gehenna.Entity
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

    fun visitFov(visitor: (Entity, Int, Int) -> Unit) {
        level.visitFov(x, y, visitor)
    }

    val neighbors: List<Entity> get() = level[x, y].filter { it != entity }

    override fun onRemove() {
        level.remove(this)
    }

    override fun onAdd() {
        level.spawn(this)
    }
}