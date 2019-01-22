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

abstract class FovLevel(size: Size) : BasicLevel(size) {
    //fov
    private val transparent = DoubleArray(size) { 0.0 }
    private val fovAlgorithm = PrecisePermissive()

    //path find
    private val navGrid = NavigationGrid(Array(size) { GridCell() }, true)
    private val pathFinderOptions = GridFinderOptions(
        true,
        false,
        ChebyshevDistance(),
        false,
        1.0F,
        1.0F
    )
    private val pathFinder = AStarGridFinder(GridCell::class.java, pathFinderOptions)

    fun findPath(from: Point, to: Point): List<Point>? {
        return pathFinder.findPath(from.x, from.y, to.x, to.y, navGrid)?.map { it.x at it.y }
    }

    fun visitFov(from: Point, range: Int, visitor: (Entity, Point) -> Unit): FovBoard {
        val fov = EntityFov(visitor)
        fovAlgorithm.visitFieldOfView(fov, from.x, from.y, range)
        return fov
    }

    fun walkableSquare(point: Point): Int {
        val visited = HashSet<GridCell>()
        fun visit(cell: GridCell) {
            visited.add(cell)
            navGrid.getNeighbors(cell, pathFinderOptions).toList().forEach {
                if (!visited.contains(it)) visit(it)
            }
        }
        visit(navGrid.getCell(point.x, point.y))
        return visited.size
    }

    override fun update(point: Point) {
        navGrid.setWalkable(point.x, point.y,
            cells[point].any { it.has<Floor>() } &&
                    cells[point].none { it<Obstacle>()?.blockPath == true })
        transparent[point] = if (cells[point].none { it<Obstacle>()?.blockView == true }) 0.0 else 1.0
    }

    abstract inner class FovBoard : ILosBoard {
        protected var board = BooleanArray(size) { false }
        operator fun get(point: Point): Boolean {
            return board[point]
        }

        override fun contains(x: Int, y: Int): Boolean {
            return (x at y) in size
        }

        override fun isObstacle(x: Int, y: Int): Boolean = transparent[x, y] == 1.0

        final override fun visit(x: Int, y: Int) {
            board[x, y] = true
            visitImpl(x at y)
        }

        abstract fun visitImpl(point: Point)

        fun clear() {
            board.fill(false)
        }

        fun isVisible(point: Point) = board[point]
    }

    private inner class EntityFov(private val visitor: (Entity, Point) -> Unit) : FovBoard() {
        override fun visitImpl(point: Point) {
            cells[point].forEach { visitor(it, point) }
        }
    }
}