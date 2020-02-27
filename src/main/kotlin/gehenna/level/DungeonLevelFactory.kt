package gehenna.level

import gehenna.component.Floor
import gehenna.component.Stairs
import gehenna.core.Context
import gehenna.utils.Point
import gehenna.utils.Point.Companion.zero
import gehenna.utils.*
import gehenna.utils.Dir.Companion.east
import gehenna.utils.Dir.Companion.north
import gehenna.utils.Dir.Companion.northeast
import gehenna.utils.Dir.Companion.northwest
import gehenna.utils.Dir.Companion.south
import gehenna.utils.Dir.Companion.southeast
import gehenna.utils.Dir.Companion.southwest
import gehenna.utils.Dir.Companion.west
import gehenna.utils.nextPoint
import gehenna.utils.random
import kotlin.random.Random

class DungeonLevelFactory(context: Context) : BaseLevelFactory<Level>(context) {

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

    private fun Level.placeStairs(previous: Level?, startPosition: Point, backPoint: Point?) {
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

    private fun Level.putDoors() {
        for (p in (size - (1 at 1)).size.range) {
            if (has(p) && has(p + southeast) && !has(p + east) && !has(p + south)) {
                part(p.x - 2 at p.y - 2, "se_connector")
            }
            if (!has(p) && !has(p + southeast) && has(p + east) && has(p + south)) {//fixme
                part(p.x - 2 at p.y - 2, "sw_connector")
            }

            if (has(p) && has(p + south) && has(p + north) && !has(p + east) && !has(p + west) &&
                    (has(p + southeast) && has(p + southwest) || has(p + northeast) && has(p + northwest))) {
                if (random.nextDouble() < 0.8) door(p)
            }

            if (has(p) && !has(p + south) && !has(p + north) && has(p + east) && has(p + west) &&
                    (has(p + southeast) && has(p + northeast) || has(p + southwest) && has(p + northwest))) {
                if (random.nextDouble() < 0.8) door(p)
            }
        }
    }

    private fun Level.cleanFloors() {
        size.range.forEach {
            while (get(it).count { entity -> entity.has<Floor>() } > 1) {
                remove(get(it).find { entity -> entity.has<Floor>() }!!) // todo: this remove double floors | maybe you can do it better
            }
        }
    }

    private fun Level.checkWalkable(startPosition: Point): Boolean {
        val floor = size.range.count { isWalkable(it) }
        return !(walkableSquare(startPosition) < floor || !isWalkable(startPosition))
    }

    override fun build(previous: Level?, backPoint: Point?): Pair<Level, Point> {
        val startPosition = backPoint ?: random.nextPoint(3, 3, 5, 5)
        return Pair(Level(size, (previous?.depth ?: -1) + 1).apply {
            //Place random walls and 3-tiles wide corridors
            for (i in 0..1000) {


                listOf("hall", "triangle", null).random()?.let { room ->
                    val part = partFactory.new(room)
                    part.spawnTo(random.nextPoint((size - part.size).size), this)
                }

//                part(10 at 10, "hall")
//                rect(0 at 0, size)
                automaton(startPosition, listOf(2 to 5 to 2, 2 to 7 to 1), 3, 0, 0.4) { has(it) }

//                automaton(startPosition, listOf(2 to 5 to 2, 2 to 7 to 1), 3, 0, 0.4)

//                automaton(startPosition, listOf(2 to 5 to 3), 3, 0, 0.45)
//                automaton(startPosition, listOf(2 to 5 to 3), 1, 4, 0.4, false)
//
//                repeat(4) {
//                    if (!checkWalkable(startPosition)) {
//                        automaton(startPosition, listOf(2 to 5 to 3), 1, 4, 0.4, false)
//                    }
//                }

                putDoors()
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
            placeStairs(previous, startPosition, backPoint)
            spawn(factory.new("stairsDown"), startPosition + (1 at 1))
        }, startPosition)
    }
}
