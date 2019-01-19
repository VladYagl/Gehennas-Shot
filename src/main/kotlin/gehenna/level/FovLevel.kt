package gehenna.level

import gehenna.component.Floor
import gehenna.component.Obstacle
import gehenna.core.Entity
import gehenna.utils.*
import org.xguzm.pathfinding.grid.GridCell
import org.xguzm.pathfinding.grid.NavigationGrid
import org.xguzm.pathfinding.grid.finders.AStarGridFinder
import org.xguzm.pathfinding.grid.finders.GridFinderOptions
import org.xguzm.pathfinding.grid.heuristics.ChebyshevDistance
import rlforj.los.ILosBoard
import rlforj.los.PrecisePermissive

abstract class FovLevel(width: Int, height: Int) : BasicLevel(width, height) {
    //fov
    private val transparent = DoubleArray(width, height) { 0.0 }
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

    fun findPath(x: Int, y: Int, toX: Int, toY: Int): List<Point>? {
        return pathFinder.findPath(x, y, toX, toY, navGrid)?.map { it.x at it.y }
    }

    fun visitFov(x: Int, y: Int, range: Int, visitor: (Entity, Int, Int) -> Unit): FovBoard {
        val fov = EntityFov(visitor)
        fovAlgorithm.visitFieldOfView(fov, x, y, range)
        return fov
    }

    fun walkableSquare(x: Int, y: Int): Int {
        val visited = HashSet<GridCell>()
        fun visit(cell: GridCell) {
            visited.add(cell)
            navGrid.getNeighbors(cell, pathFinderOptions).toList().forEach {
                if (!visited.contains(it)) visit(it)
            }
        }
        visit(navGrid.getCell(x, y))
        return visited.size
    }

    override fun update(x: Int, y: Int) {
        navGrid.setWalkable(x, y,
            cells[x, y].any { it.has<Floor>() } &&
                    cells[x, y].none { it<Obstacle>()?.blockPath == true })
        transparent[x, y] = if (cells[x, y].none { it<Obstacle>()?.blockView == true }) 0.0 else 1.0
    }

    abstract inner class FovBoard : ILosBoard {
        protected var board = BooleanArray(width, height) { false }
        operator fun get(x: Int, y: Int): Boolean {
            return board[x, y]
        }

        override fun contains(x: Int, y: Int): Boolean {
            return (x in 0 until width) && (y in 0 until height)
        }

        override fun isObstacle(x: Int, y: Int): Boolean = transparent[x, y] == 1.0

        final override fun visit(x: Int, y: Int) {
            board[x, y] = true
            visitImpl(x, y)
        }

        abstract fun visitImpl(x: Int, y: Int)

        fun clear() {
            board.fill(false)
        }

        fun isVisible(x: Int, y: Int) = board[x, y]
    }

    private inner class EntityFov(private val visitor: (Entity, Int, Int) -> Unit) : FovBoard() {
        override fun visitImpl(x: Int, y: Int) {
            cells[x, y].forEach { visitor(it, x, y) }
        }
    }
}