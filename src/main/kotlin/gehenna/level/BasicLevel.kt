package gehenna.level

import gehenna.Entity
import gehenna.components.Obstacle
import gehenna.components.Position
import gehenna.utils.Array
import gehenna.utils.*

abstract class BasicLevel(val width: Int, val height: Int) {
    protected val cells = Array(width, height) { HashSet<Entity>() }

    operator fun get(x: Int, y: Int): HashSet<Entity> {
        return cells[x, y]
    }

    fun spawn(entity: Entity, x: Int, y: Int) {
        val pos = Position(entity, x, y, this as Level) // xd looks no good
        entity.add(pos)
    }

    fun spawn(pos: Position) {
        cells[pos.x, pos.y].add(pos.entity)
        update(pos.x, pos.y)
    }

    fun remove(entity: Entity) {
        val pos = entity[Position::class]!!
        //cells[pos.x, pos.y].remove(entity)
        entity.remove(pos)
        //update(pos.x, pos.y)
    }

    fun remove(pos: Position) {
        cells[pos.x, pos.y].remove(pos.entity)
        update(pos.x, pos.y)
    }

    fun move(entity: Entity, x: Int, y: Int) {
        remove(entity)
        spawn(entity, x, y)
    }

    fun obstacle(x: Int, y: Int): Entity? {
        return cells[x][y].firstOrNull { it[Obstacle::class]?.blockMove ?: false }
    }

    fun isBlocked(x: Int, y: Int): Boolean {
        return cells[x, y].any { it[Obstacle::class]?.blockMove ?: false }
    }

    protected open fun update(x: Int, y: Int) {}
}