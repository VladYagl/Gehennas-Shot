package gehenna.level

import gehenna.component.Floor
import gehenna.component.Obstacle
import gehenna.core.Context
import gehenna.utils.Point
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

class CaveLevelFactory(context: Context) : BaseLevelFactory<Level>(context) {

    override fun build(previous: Level?, backPoint: Point?): Pair<Level, Point> {
        val startPosition = backPoint ?: random.nextPoint(3, 3, 5, 5)
        return Pair(Level(size, (previous?.depth ?: -1) + 1).apply {
            buildLoop(startPosition) {

                rect(startPosition - (2 at 2), Size(4, 4))

//                listOf("dog_cave").random().let { room ->
                listOf(
                        "dog_cave" to 0.2,
                        "hall" to 0.1,
                        (null as String?) to 0.7
                ).random()?.let { room ->
                    val part = partFactory.new(room)
                    part.spawnTo(random.nextPoint((size - part.size).size), this)
                }

                automaton(startPosition, listOf(4 to 4 to 5), 1, 0, 0.5) { has(it) }

                filterBadComponents(40)
            }

            placeStairs(previous, startPosition, backPoint)
        }, startPosition)
    }
}

class CorridorLevelFactory(context: Context) : BaseLevelFactory<Level>(context) {

    override val defaultLight: Int = 2

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

    override fun build(previous: Level?, backPoint: Point?): Pair<Level, Point> {
        val startPosition = backPoint ?: random.nextPoint(3, 3, 5, 5)
        return Pair(Level(size, (previous?.depth ?: -1) + 1).apply {
            buildLoop(startPosition) {

                rect(startPosition - (2 at 2), Size(4, 4))

                listOf(
                        "triangle" to 0.2,
                        (null as String?) to 0.8
                ).random()?.let { room ->
                    val part = partFactory.new(room)
                    part.spawnTo(random.nextPoint((size - part.size).size), this)
                }

                automaton(startPosition, listOf(2 to 5 to 2, 2 to 7 to 1), 3, 0, 0.4) {
                    safeGet(it).none { it<Obstacle>()?.blockView == true } && safeGet(it).any { it.has<Floor>() }
                }

                putDoors()
            }

            placeStairs(previous, startPosition, backPoint)
        }, startPosition)
    }
}

class DungeonLevelFactory(context: Context) : BaseLevelFactory<Level>(context) {
    private val factories = listOf(
            CaveLevelFactory(context),
            CorridorLevelFactory(context)
    )

    override var size: Size = super.size
        set(value) {
            factories.forEach { it.size = value }
            field = value
        }

    override fun build(previous: Level?, backPoint: Point?): Pair<Level, Point> {
        return factories.random().build(previous, backPoint)
    }
}
