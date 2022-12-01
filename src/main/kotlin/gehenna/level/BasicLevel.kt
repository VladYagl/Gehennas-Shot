package gehenna.level

import gehenna.component.Floor
import gehenna.component.Glyph
import gehenna.component.Obstacle
import gehenna.component.Position
import gehenna.core.Entity
import gehenna.utils.*
import java.io.Serializable

/**
 * Basic level with 2-d array of cells and 2-d array of memory
 */
abstract class BasicLevel(val size: Size) : Serializable {
    protected val cells = Array(size) { HashSet<Entity>() }
    private var memory = Array(size) { 0L to null as Glyph? }

    /**
     * Returns all entities on the level
     */
    fun getAll(): List<Entity> {
        return cells.flatten().flatten()
    }

    /**
     * Returns all entities in cell
     */
    operator fun get(point: Point): Set<Entity> {
        return cells[point]
    }

    fun memory(point: Point): Glyph? {
        return if (inBounds(point)) memory[point].second else null
    }

    fun remember(point: Point, glyph: Glyph, time: Long) {
        if (time > memory[point].first || glyph.priority > (memory[point].second?.priority ?: Int.MIN_VALUE))
            memory[point] = time to glyph
    }

    fun inBounds(point: Point): Boolean {
        return point in size
    }

    fun safeGet(point: Point): Set<Entity> {
        return if (inBounds(point)) cells[point] else emptySet()
    }

    fun spawn(entity: Entity, at: Point, lastPoint: Point? = null) {
        Position(at, this as Level, entity, lastPoint?.plus(0 at 0)).attach()
    }

    fun spawn(pos: Position) {
        cells[pos.x, pos.y].add(pos.entity)
        update(pos)
    }

    fun remove(entity: Entity) {
        entity.remove<Position>()
    }

    fun remove(pos: Position) {
        cells[pos.x, pos.y].remove(pos.entity)
        update(pos)
    }

    fun move(entity: Entity, to: Point) {
        val lastPoint = entity<Position>()
        remove(entity)
        spawn(entity, to, lastPoint)
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