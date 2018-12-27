import org.xguzm.pathfinding.grid.GridCell
import org.xguzm.pathfinding.grid.NavigationGrid
import org.xguzm.pathfinding.grid.finders.AStarGridFinder
import rlforj.los.ILosBoard
import rlforj.los.PrecisePermissive
import org.xguzm.pathfinding.grid.finders.GridFinderOptions
import org.xguzm.pathfinding.grid.heuristics.ChebyshevDistance
import utils.*

class Level(val width: Int, val height: Int, val factory: EntityFactory) : ILosBoard {
    private val cells = Array(width, height) { HashSet<Entity>() }

    //fov
    private val transparent = DoubleArray(width, height) { 0.0 }
    private var fov = BooleanArray(width, height) { false }
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

    override fun contains(x: Int, y: Int): Boolean {
        return (x in 0 until width) && (y in 0 until height)
    }

    override fun isObstacle(x: Int, y: Int): Boolean = transparent[x, y] == 1.0

    override fun visit(x: Int, y: Int) {
        fov[x, y] = true
        val glyph = cells[x, y].maxBy { it[Glyph::class]?.priority ?: Int.MIN_VALUE }?.get(Glyph::class)
        if (glyph != null && glyph.memorable) {
            memory[x, y] = glyph
        }
    }

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
//        cells[pos.x, pos.y].remove(entity)
        entity.remove(pos)
//        update(pos.x, pos.y)
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

    fun updateFOV(x: Int, y: Int) {
        fov.fill(false)
        fovAlgorithm.visitFieldOfView(this, x, y, 25)
    }

    private fun update(x: Int, y: Int) {
        navGrid.setWalkable(x, y,
            cells[x, y].any { it.has(Floor::class) } &&
                    cells[x, y].none { it[Obstacle::class]?.blockPath == true })
        transparent[x, y] = if (cells[x, y].none { it[Obstacle::class]?.blockView == true }) 0.0 else 1.0
    }

    private fun wall(x: Int, y: Int) {
        spawn(factory.newEntity("wall"), x, y)
    }

    private fun floor(x: Int, y: Int) {
        spawn(factory.newEntity("floor"), x, y)
    }

    private fun room(x1: Int, y1: Int, width: Int, height: Int) {
        for (x in x1 until x1 + width) {
            for (y in y1 until y1 + height) {
                if (cells[x, y].none { it.has(Floor::class) }) {
                    floor(x, y)
                }
                if (x == x1 || x == x1 + width - 1 || y == y1 || y == y1 + height - 1) {
                    wall(x, y)
                }
            }
        }
    }

    fun init() {
        room(0, 0, width, height)
        room(13, 15, 10, 20)
        remove(cells[22, 21].find { it.has(Obstacle::class) }!!)
        spawn(factory.newEntity("bandit"), 15, 18)
    }
}
