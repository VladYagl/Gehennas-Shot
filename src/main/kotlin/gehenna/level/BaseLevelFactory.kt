package gehenna.level

import gehenna.component.Floor
import gehenna.component.Obstacle
import gehenna.component.Stairs
import gehenna.core.Context
import gehenna.core.Entity
import gehenna.factory.Factory
import gehenna.factory.LevelPart
import gehenna.utils.*
import gehenna.utils.Point.Companion.zero
import kotlin.math.pow
import kotlin.random.Random

abstract class BaseLevelFactory<T : Level>(protected val context: Context) : LevelFactory<T> {
    protected var factory: Factory<Entity> = context.factory
    protected var partFactory: Factory<LevelPart> = context.partFactory
    open var size: Size = Size(5 * 8, 6 * 8)

    final override fun new(previous: Level?, backPoint: Point?): Pair<T, Point> {
        val (level, point) = this.build(previous, backPoint)
        context.levels.add(level)
        return Pair(level, point)
    }

    abstract fun build(previous: Level?, backPoint: Point?): Pair<T, Point>

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

    protected fun Level.door(at: Point) {
        spawn(factory.new("door"), at)
    }

    protected fun Level.floor(at: Point) {
        spawn(factory.new("floor"), at)
    }

    protected fun Level.automaton(
            point: Point,
            rules: List<Triple<Int, Int, Int>>,
            scale: Int = 3,
            offset: Int = 0,
            initChance: Double = 0.4,
            initEmpty: Boolean = true,
            random: Random = gehenna.utils.random,
            ignore: (Point) -> Boolean = { false }
    ) {
        //Real -> spawns floor, cellular -> scales to real
        val real = CellularPart(size) { floor(it) }
        val trueScale = scale + offset * 2
        for ((i, j) in size.range) real.cells[i, j] = true
        val cellular = CellularPart(Size(size.width / trueScale, size.height / trueScale), {
            ignore(it.x / trueScale at it.y / trueScale)
        }) {
            for ((i, j) in Size(scale, scale).range) {
                real.cells[trueScale * it.x + i + offset, trueScale * it.y + j + offset] = false
            }

            for ((i, j) in Size(offset, scale).range) {
                real.cells[trueScale * it.x + i, trueScale * it.y + j + offset] = false
                real.cells[trueScale * it.x + i + scale + offset, trueScale * it.y + j + offset] = false
                real.cells[trueScale * it.x + j + offset, trueScale * it.y + i] = false
                real.cells[trueScale * it.x + j + offset, trueScale * it.y + i + scale + offset] = false
            }
        }

        //Initialises randomly
        for ((i, j) in Size(size.width / trueScale, size.height / trueScale).range) {
            if (random.nextDouble() < initChance) cellular.cells[i, j] = true
        }

        //Play life for each rules
        rules.forEach {
            cellular.automaton(it.first, it.second, it.third)
        }
        if (initEmpty) {
            cellular.cells[point.x / trueScale, point.y / trueScale] = false
        }
        cellular.spawnTo(zero, this)
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

    protected fun Level.clear(point: Point) {
        get(point).toList().forEach {
            remove(it)
            it.clean()
        }
    }

    protected fun Level.clear() {
        for ((x, y) in size.range) {
            clear(x at y)
        }
    }

    private fun Level.cleanFloors() {
        size.range.forEach {
            while (get(it).count { entity -> entity.has<Floor>() } > 1) {
                remove(get(it).find { entity -> entity.has<Floor>() }!!) // todo: this remove double floors | maybe you can do it better
            }
        }
    }

    protected fun Level.checkWalkable(startPosition: Point): Boolean {
        val floor = size.range.count { isWalkable(it) }
        return !(walkableSquare(startPosition) < floor || !isWalkable(startPosition))
    }

    protected fun Level.filterBadComponents(threshold: Int): Pair<Int, Array<IntArray>> {
        val colors = IntArray(size);
        var color = 0
        for (point in size.range) {
            if (isWalkable(point) && colors[point] == 0) {
                color++
                var size = 0
                visitWalkable(point) {
                    colors[it] = color
                    size++
                }

                if (size < threshold) {
                    visitWalkable(point) {
                        clear(it)
                    }
                }
            }
        }

        return Pair(color, colors)
    }

    protected fun Level.placeStairs(previous: Level?, startPosition: Point, backPoint: Point?) {
        //TODO: change it -> level factory can manage stairs down
        while (true) {
            val point = random.nextPoint(size)
            if (isWalkable(point) && findPath(startPosition, point)?.size ?: 0 > 25) {
                spawn(factory.new("stairsDown"), point)
                break
            }
        }

        previous?.let {
            backPoint?.let {
                val stairs = factory.new("stairsUp")
                stairs<Stairs>()?.destination = previous to backPoint
                spawn(stairs, startPosition)
            }
        }
    }

    private fun Level.spawnEnemies() {
        //Place bandits depending on level
        if (depth == 0) repeat(random.nextInt(6) + 4) {
            while (true) {
                val point = random.nextPoint(size)
                if (isWalkable(point)) {
                    spawn(factory.new("bandit"), point)
                    break
                }
            }
        }
        else repeat(random.nextInt(6) + 4) {
            while (true) {
                val point = random.nextPoint(size)
                if (isWalkable(point)) {
                    spawn(factory.new("strongBandit"), point)
                    break
                }
            }
        }
    }

    protected fun Level.buildLoop(startPosition: Point, build: () -> Unit) {
        for (i in 0..1000) {
            build()
            cleanFloors()

            if (!checkWalkable(startPosition)) {
                println("Level is not connected, trying again: $i")
                clear()
            } else break
//                break
        }
        require(checkWalkable(startPosition)) { "Can't generate adequate level(" }

        box(zero, size)
        allWalls()

        spawnEnemies()
    }
}