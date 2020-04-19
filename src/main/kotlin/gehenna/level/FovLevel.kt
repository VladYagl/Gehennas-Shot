package gehenna.level

import gehenna.component.Floor
import gehenna.component.LightSource
import gehenna.component.Obstacle
import gehenna.component.Position
import gehenna.core.Entity
import gehenna.utils.*
import org.xguzm.pathfinding.grid.GridCell
import org.xguzm.pathfinding.grid.NavigationGrid
import org.xguzm.pathfinding.grid.finders.AStarGridFinder
import org.xguzm.pathfinding.grid.finders.GridFinderOptions
import org.xguzm.pathfinding.grid.heuristics.ChebyshevDistance
import rlforj.los.BresLos
import rlforj.los.ILosBoard
import rlforj.los.PrecisePermissive
import java.io.ObjectInputStream
import kotlin.math.roundToInt
import kotlin.math.sqrt

abstract class FovLevel(size: Size) : BasicLevel(size) {
    //fov
    private val transparent = DoubleArray(size) { 0.0 }

    @Transient
    private val fovAlgorithm = PrecisePermissive()

    val light = Array(size) { 0 }

    fun distance(a: Point, b: Point): Int {
        return sqrt((a.x - b.x) * (a.x - b.x).toDouble() + (a.y - b.y) * (a.y - b.y)).roundToInt()
    }

    fun updateLight() {
        for (pos in size.range) {
            light[pos] = 0
        }

        getAll().mapNotNull { it<LightSource>() }.forEach {
            val pos: Point = it.entity.one<Position>()
            visitFov(pos, it.intensity) { _, point ->
                light[point] += kotlin.math.max(it.intensity - distance(pos, point), 0)
            }
        }
    }

    //path find
    @Transient
    private val navGrid = NavGrid(Array(size) { GridCell() }, true)

    @Transient
    private val pathFinderOptions = GridFinderOptions(
            true,
            false,
            ChebyshevDistance(),
            false,
            1.0F,
            1.0F
    )

    @Transient
    private val pathFinder = AStarGridFinder(GridCell::class.java, pathFinderOptions)

    fun findPath(from: Point, to: Point): List<Point>? {
        return pathFinder.findPath(from.x, from.y, to.x, to.y, navGrid)?.map { it.x at it.y }
    }

    fun visitFov(from: Point, range: Int, visitor: (Entity, Point) -> Unit): FovBoard {
        val fov = EntityFov(visitor)
        fovAlgorithm.visitFieldOfView(fov, from.x, from.y, range)
        return fov
    }

    fun getLOS(from: Point, to: Point): List<Point>? {
//        val los = fovAlgorithm
        val los = BresLos(true)
        return if (los.existsLineOfSight(EntityFov { _, _ -> }, from.x, from.y, to.x, to.y, true)) {
            los.projectPath.map {
                it.x at it.y
            }
        } else {
            null
        }
    }

    fun walkableSquare(point: Point): Int {
        var cnt = 0;
        visitWalkable(point) { cnt++ }
        return cnt
    }

    fun visitWalkable(point: Point, visit: (Point) -> Unit) {
        val visited = HashSet<GridCell>()
        fun dfs(cell: GridCell) {
            visited.add(cell)
            visit(cell.x at cell.y)
            navGrid.getNeighbors(cell, pathFinderOptions).toList().forEach {
                if (!visited.contains(it)) dfs(it)
            }
        }
        dfs(navGrid.getCell(point.x, point.y))
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

    private fun readObject(inputStream: ObjectInputStream) {
        inputStream.defaultReadObject()

        //TODO: copy pasta of default values -- this is not good pls fix
        this.setVal("fovAlgorithm", PrecisePermissive())
        this.setVal("navGrid", NavGrid(Array(size) { GridCell() }, true))
        this.setVal("pathFinderOptions", GridFinderOptions(
                true,
                false,
                ChebyshevDistance(),
                false,
                1.0F,
                1.0F
        ))
        this.setVal("pathFinder", AStarGridFinder(GridCell::class.java, pathFinderOptions))
    }
}