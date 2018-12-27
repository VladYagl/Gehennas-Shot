package gehenna

import gehenna.components.*
import gehenna.utils.*
import org.xguzm.pathfinding.grid.GridCell
import org.xguzm.pathfinding.grid.NavigationGrid
import org.xguzm.pathfinding.grid.finders.AStarGridFinder
import rlforj.los.ILosBoard
import rlforj.los.PrecisePermissive
import org.xguzm.pathfinding.grid.finders.GridFinderOptions
import org.xguzm.pathfinding.grid.heuristics.ChebyshevDistance

open class Level(val width: Int, val height: Int, val factory: EntityFactory) {
    protected val cells = Array(width, height) { HashSet<Entity>() }

    //fov
    private val transparent = DoubleArray(width, height) { 0.0 }
    private val fov = FovBoard()
    private var memory = Array(width, height) { null as Glyph? }
    private val fovAlgorithm = PrecisePermissive()

    //path find
    private val navGrid = NavigationGrid(Array(width, height) { GridCell() }, true)
    private val pathFinderOptions = GridFinderOptions(
        true,
        false,
        ChebyshevDistance(),
        false,
        1.0F,
        1.0F
    )
    private val pathFinder = AStarGridFinder(GridCell::class.java, pathFinderOptions)

    @Deprecated("DEBUG") // TODO!!!
    fun findPath(x: Int, y: Int): List<Pair<Int, Int>>? {
        var pos: Position? = null
        cells.forEach { row ->
            row.forEach { entities ->
                entities.forEach {
                    if (it.name == "player") {
                        pos = it[Position::class]
                    }
                }
            }
        }
        return findPath(x, y, pos!!.x, pos!!.y)
    }

    fun findPath(x: Int, y: Int, toX: Int, toY: Int): List<Pair<Int, Int>>? {
        return pathFinder.findPath(x, y, toX, toY, navGrid)?.map { it.x to it.y }
    }

    operator fun get(x: Int, y: Int): HashSet<Entity> {
        return cells[x, y]
    }

    fun spawn(entity: Entity, x: Int, y: Int) {
        val pos = Position(entity, x, y, this)
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

    fun isVisible(x: Int, y: Int): Boolean {
        return fov[x, y]
    }

    fun memory(x: Int, y: Int): Glyph? {
        return memory[x, y]
    }

    fun visitFOV(x: Int, y: Int, visitor: (Glyph, Int, Int) -> Unit = { _, _, _ -> }) {
        fov.visitor = visitor
        fov.clear()
        fovAlgorithm.visitFieldOfView(fov, x, y, 25)
    }

    private fun update(x: Int, y: Int) {
        navGrid.setWalkable(x, y,
            cells[x, y].any { it.has(Floor::class) } &&
                    cells[x, y].none { it[Obstacle::class]?.blockPath == true })
        transparent[x, y] = if (cells[x, y].none { it[Obstacle::class]?.blockView == true }) 0.0 else 1.0
    }

    private inner class FovBoard : ILosBoard {
        private var board = BooleanArray(width, height) { false }
        operator fun get(x: Int, y: Int): Boolean {
            return board[x, y]
        }

        var visitor: (Glyph, Int, Int) -> Unit = { _, _, _ -> }

        override fun contains(x: Int, y: Int): Boolean {
            return (x in 0 until width) && (y in 0 until height)
        }

        override fun isObstacle(x: Int, y: Int): Boolean = transparent[x, y] == 1.0

        override fun visit(x: Int, y: Int) {
            board[x, y] = true
            val glyph = cells[x, y].maxBy { it[Glyph::class]?.priority ?: Int.MIN_VALUE }?.get(Glyph::class)
            if (glyph != null && glyph.memorable) {
                memory[x, y] = glyph
            }
            cells[x, y].forEach { it[Glyph::class]?.let { glyph -> visitor(glyph, x, y) } }
        }

        fun clear() {
            board.fill(false)
        }
    }
}


