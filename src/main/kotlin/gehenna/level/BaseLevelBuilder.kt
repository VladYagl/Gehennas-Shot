package gehenna.level

import gehenna.component.Floor
import gehenna.component.Obstacle
import gehenna.core.Entity
import gehenna.factory.Factory
import gehenna.factory.LevelPart
import gehenna.utils.*
import kotlin.math.abs
import kotlin.random.Random

abstract class BaseLevelBuilder<T : Level> : LevelBuilder<T> {
    private class EmptyFactory<T> : Factory<T> {
        override fun new(name: String): T = throw Exception("this is empty factory")
    }

    protected var factory: Factory<Entity> = EmptyFactory()
    protected var partFactory: Factory<LevelPart> = EmptyFactory()
    protected var width: Int = 5 * 8
    protected var height: Int = 6 * 8

    protected var previous: Level? = null
    protected var backPoint: Point? = null

    override fun withPrevious(level: Level) = also { this.previous = level }

    override fun withBackPoint(point: Point) = also { this.backPoint = point }

    fun withFactory(factory: Factory<Entity>) = also { this.factory = factory }

    fun withPartFactory(partFactory: Factory<LevelPart>) = also { this.partFactory = partFactory }

    fun withSize(width: Int, height: Int) = also {
        this.width = width
        this.height = height
    }

    protected fun Level.corridor(x: Int, y: Int, dir: Point, len: Int = 0, door: Boolean = false) {
        if (!inBounds(x, y) || get(x, y).isNotEmpty()) return
        floor(x, y)
        if (door) spawn(factory.new("door"), x, y)
        var newLen = len + 1
        if (!door) {
            val d = turnLeft(turnLeft(dir))
            var p = (x to y) + d
            if (inBounds(p.x, p.y) && get(p.x, p.y).isEmpty()) floor(p.x, p.y)
            p = (x to y) - d
            if (inBounds(p.x, p.y) && get(p.x, p.y).isEmpty()) floor(p.x, p.y)
        }
        if (len > 8)
            if (Random.nextDouble() > 0.8) {
                val tempDir = random.next4way(dir)
                corridor(x + tempDir.x * 2, y + tempDir.y * 2, tempDir, 0, true)
                newLen = 3
            }

        if (len > 16)
            if (Random.nextDouble() > 0.8) {
                val newDir = random.next4way(dir)
                corridor(x + newDir.x * 2, y + newDir.y * 2, newDir, 0, true)
                return
            }
        corridor(x + dir.x, y + dir.y, dir, newLen, false)
    }

    protected fun Level.allWalls() {
        for ((x, y) in range(width, height)) {
            if (get(x, y).isEmpty())
                for (dir in directions) {
                    if (safeGet(x + dir.x, y + dir.y).isNotEmpty() && safeGet(
                            x + dir.x,
                            y + dir.y
                        ).none { it[Obstacle::class]?.blockMove == true }
                    ) {
                        wall(x, y)
                        break
                    }
                }
        }
    }

    protected fun Level.wall(x: Int, y: Int) {
        spawn(factory.new("wall"), x, y)
    }

    protected fun Level.floor(x: Int, y: Int) {
        spawn(factory.new("floor"), x, y)
    }

    protected fun Level.automaton(x: Int, y: Int, k: Int) {
//        val random = Random(seed)
        val real = CellularPart(width, height) { x1, y1 -> floor(x1, y1) }
        for ((i, j) in range(width, height)) real.cells[i, j] = true
        val cellular = CellularPart(width / 3, height / 3) { x1, y1 ->
            for ((i, j) in range(3, 3)) {
//                floor(3 * x1 + i, 3 * y1 + j)
                real.cells[3 * x1 + i, 3 * y1 + j] = false
            }
        }
        for ((i, j) in range(width / 3, height / 3)) {
            fun norm(x: Int) = Math.pow(x.toDouble(), 0.5)
            val d = norm(abs(i - x / 3) + abs(y / 3 - j))
//            cellular.cells[i, j] = random.nextDouble() >= norm(width / 3 + height / 3) / d * 0.2 + 0.1
            if (Random.nextDouble() < 0.4) cellular.cells[i, j] = true
        }
        cellular.automaton(2, 5, 2)
        cellular.automaton(2, 8, 1)
        cellular.cells[x / 3, y / 3] = false
        cellular.spawnTo(0, 0, this)
//        real.automaton(4, 4, 1)
        real.spawnTo(0, 0, this)
    }

    protected fun Level.rect(x1: Int, y1: Int, width: Int, height: Int) {
        for ((x, y) in (x1 to y1) until (x1 + width to y1 + height)) {
            if (get(x, y).none { it.has(Floor::class) }) {
                floor(x, y)
            }
        }
    }

    protected fun Level.box(x1: Int, y1: Int, width: Int, height: Int) {
        for ((x, y) in (x1 to y1) until (x1 + width to y1 + height)) {
            if (x == x1 || x == x1 + width - 1 || y == y1 || y == y1 + height - 1) {
                wall(x, y)
            }
        }
    }

    protected fun Level.part(x: Int, y: Int, name: String) {
        partFactory.new(name).spawnTo(x, y, this)
    }

    protected fun Level.has(x: Int, y: Int): Boolean {
        return safeGet(x, y).isNotEmpty()
    }
}