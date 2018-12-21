import rlforj.los.ILosBoard
import rlforj.los.PrecisePermissive

class Level(val width: Int, val height: Int, val factory: EntityFactory) : ILosBoard {
    private val cells = Array(width, height) { HashSet<Entity>() }
    private val walkable = BooleanArray(width, height) { false }
    private val transparent = DoubleArray(width, height) { 0.0 }
    private var fov = BooleanArray(width, height) { false }
    private var memory = Array(width, height) { null as Glyph? }
    private val fovAlgorithm = PrecisePermissive()

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

    fun spawn(entity: Entity, x: Int, y: Int) {
        cells[x, y].add(entity)
        val pos = Position(entity, x, y, this)
        entity.add(pos)
        update(x, y)
    }

    fun remove(entity: Entity) {
        val pos = entity[Position::class]!!
        cells[pos.x, pos.y].remove(entity)
        entity.remove(pos)
        update(pos.x, pos.y)
    }
/**/
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
        walkable[x, y] = cells[x, y].any { it.has(Floor::class) } &&
                cells[x, y].none { it[Obstacle::class]?.blockMove == true }
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

    init {
        room(0, 0, width, height)
        room(13, 15, 10, 20)
        remove(cells[22, 21].find { it.has(Obstacle::class) }!!)
        spawn(factory.newEntity("bandit"), 15, 18)
    }
}
