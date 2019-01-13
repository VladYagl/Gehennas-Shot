package gehenna.level

import gehenna.component.Floor
import gehenna.component.Glyph
import gehenna.component.Obstacle
import gehenna.component.Position
import gehenna.core.Entity
import gehenna.utils.*

abstract class BasicLevel(val width: Int, val height: Int) {
    abstract val startPosition: Point
    protected val cells = Array(width, height) { HashSet<Entity>() }
    private var memory = Array(width, height) { 0L to null as Glyph? }

    operator fun get(x: Int, y: Int): Set<Entity> {
        return cells[x, y]
    }

    fun memory(x: Int, y: Int): Glyph? {
        return if (inBounds(x, y)) memory[x, y].second else null
    }

    fun remember(x: Int, y: Int, glyph: Glyph, time: Long) {
        if (time > memory[x, y].first || glyph.priority > memory[x, y].second?.priority ?: Int.MIN_VALUE)
            memory[x, y] = time to glyph
    }

    fun inBounds(x: Int, y: Int): Boolean {
        return (x >= 0 && y >= 0 && x < width && y < height)
    }

    fun safeGet(x: Int, y: Int): Set<Entity> {
        return if (inBounds(x, y)) cells[x, y] else emptySet()
    }

    fun spawnAtStart(entity: Entity) = spawn(entity, startPosition)

    fun spawn(entity: Entity, point: Point) = spawn(entity, point.x, point.y)

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
        entity.remove(pos)
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

    fun isWalkable(x: Int, y: Int): Boolean {
        return cells[x, y].any { it.has(Floor::class) } &&
                cells[x, y].none { it[Obstacle::class]?.blockMove == true }
    }

    fun isBlocked(x: Int, y: Int): Boolean {
        return cells[x, y].any { it[Obstacle::class]?.blockMove ?: false }
    }

    open fun update(x: Int, y: Int) {}
}