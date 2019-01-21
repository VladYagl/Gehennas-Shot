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

    operator fun get(point: Point): Set<Entity> {
        return cells[point]
    }

    fun memory(point: Point): Glyph? {
        return if (inBounds(point)) memory[point].second else null
    }

    fun remember(point: Point, glyph: Glyph, time: Long) {
        if (time > memory[point].first || glyph.priority > memory[point].second?.priority ?: Int.MIN_VALUE)
            memory[point] = time to glyph
    }

    fun inBounds(point: Point): Boolean {
        val (x, y) = point
        return (x >= 0 && y >= 0 && x < width && y < height)
    }

    fun safeGet(point: Point): Set<Entity> {
        return if (inBounds(point)) cells[point] else emptySet()
    }

    fun spawnAtStart(entity: Entity) = spawn(entity, startPosition)

    fun spawn(entity: Entity, at: Point) {
        entity.add(Position(at, this as Level, entity))
    }

    fun spawn(pos: Position) {
        cells[pos.x, pos.y].add(pos.entity)
        update(pos)
    }

    fun remove(entity: Entity) {
        val pos = entity<Position>()!!
        entity.remove(pos)
    }

    fun remove(pos: Position) {
        cells[pos.x, pos.y].remove(pos.entity)
        update(pos)
    }

    fun move(entity: Entity, to: Point) {
        remove(entity)
        spawn(entity, to)
    }

    fun obstacle(point: Point): Entity? {
        return cells[point].firstOrNull { it<Obstacle>()?.blockMove ?: false }
    }

    fun isWalkable(point: Point): Boolean {
        return cells[point].any { it.has<Floor>() } &&
                cells[point].none { it<Obstacle>()?.blockMove == true }
    }

    fun isBlocked(point: Point): Boolean {
        return cells[point].any { it<Obstacle>()?.blockMove ?: false }
    }

    open fun update(point: Point) {}
}