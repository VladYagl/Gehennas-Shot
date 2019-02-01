package gehenna.level

import gehenna.component.Floor
import gehenna.component.Obstacle
import gehenna.core.Entity
import gehenna.factory.Factory
import gehenna.factory.LevelPart
import gehenna.utils.*
import gehenna.utils.Point.Companion.zero
import kotlin.math.abs
import kotlin.random.Random

abstract class BaseLevelBuilder<T : Level> : LevelBuilder<T> {
    private class EmptyFactory<T> : Factory<T> {
        override fun new(name: String): T = throw NotImplementedError("This is empty factory")
    }

    protected var factory: Factory<Entity> = EmptyFactory()
    protected var partFactory: Factory<LevelPart> = EmptyFactory()
    protected var size: Size = Size(5 * 8, 6 * 8)

    protected var previous: Level? = null
    protected var backPoint: Point? = null

    override fun withPrevious(level: Level) = also { this.previous = level }

    override fun withBackPoint(point: Point) = also { this.backPoint = point }

    fun withFactory(factory: Factory<Entity>) = also { this.factory = factory }

    fun withPartFactory(partFactory: Factory<LevelPart>) = also { this.partFactory = partFactory }

    fun withSize(size: Size) = also { this.size = size }

    fun withSize(width: Int, height: Int) = also { this.size = Size(width, height) }

    protected fun Level.corridor(from: Point, dir: Dir, len: Int = 0, door: Boolean = false) {
        if (!inBounds(from) || get(from).isNotEmpty()) return
        floor(from)
        if (door) spawn(factory.new("door"), from)
        var newLen = len + 1
        if (!door) {
            val d = dir.turnLeft.turnLeft
            var doorP = from + d
            if (inBounds(doorP) && get(doorP).isEmpty()) floor(doorP)
            doorP = from - d
            if (inBounds(doorP) && get(doorP).isEmpty()) floor(doorP)
        }
        if (len > 8)
            if (Random.nextDouble() > 0.8) {
                val tempDir = random.next4way(dir)
                corridor(from.x + tempDir.x * 2 at from.y + tempDir.y * 2, tempDir, 0, true)
                newLen = 3
            }

        if (len > 16)
            if (Random.nextDouble() > 0.8) {
                val newDir = random.next4way(dir)
                corridor(from.x + newDir.x * 2 at from.y + newDir.y * 2, newDir, 0, true)
                return
            }
        corridor(from + dir, dir, newLen, false)
    }

    protected fun Level.allWalls() {
        for (point in size.range) {
            if (get(point).isEmpty())
                for (dir in Dir) {
                    if (safeGet((point) + dir).isNotEmpty()
                            && safeGet((point) + dir).none { it<Obstacle>()?.blockMove == true }
                    ) {
                        wall(point)
                        break
                    }
                }
        }
    }

    protected fun Level.wall(at: Point) {
        spawn(factory.new("wall"), at)
    }

    protected fun Level.floor(at: Point) {
        spawn(factory.new("floor"), at)
    }

    protected fun Level.automaton(point: Point, k: Int) {
//        val random = Random(seed)
        val real = CellularPart(size) { floor(it) }
        for ((i, j) in size.range) real.cells[i, j] = true
        val cellular = CellularPart(Size(size.width / 3, size.height / 3)) {
            for ((i, j) in Size(3, 3).range) {
//                floor(3 * x1 + i, 3 * y1 + j)
                real.cells[3 * it.x + i, 3 * it.y + j] = false
            }
        }
        for ((i, j) in Size(size.width / 3, size.height / 3).range) {
            fun norm(x: Int) = Math.pow(x.toDouble(), 0.5)
            val d = norm(abs(i - point.x / 3) + abs(point.y / 3 - j))
//            cellular.cells[i, j] = random.nextDouble() >= norm(width / 3 + height / 3) / d * 0.2 + 0.1
            if (Random.nextDouble() < 0.4) cellular.cells[i, j] = true
        }
        cellular.automaton(2, 5, 2)
        cellular.automaton(2, 7, 1)
        cellular.cells[point.x / 3, point.y / 3] = false
        cellular.spawnTo(zero, this) // todo zero point
//        real.automaton(8, 5, 1)
        real.spawnTo(zero, this)
    }

    protected fun Level.rect(point: Point, size: Size) {
        for (pos in point until (point + size)) {
            if (get(pos).none { it.has<Floor>() }) {
                floor(pos)
            }
        }
    }

    protected fun Level.box(point: Point, size: Size) {
        for ((x, y) in (point) until (point + size)) {
            if (x == point.x || x == point.x + size.width - 1 || y == point.y || y == point.y + size.height - 1) {
                wall(x at y)
            }
        }
    }

    protected fun Level.part(point: Point, name: String) {
        partFactory.new(name).spawnTo(point, this)
    }

    protected fun Level.has(point: Point): Boolean {
        return safeGet(point).isNotEmpty()
    }

    protected fun Level.clear() {
        for ((x, y) in size.range) {
            get(x at y).toList().forEach { remove(it) }
        }
    }
}