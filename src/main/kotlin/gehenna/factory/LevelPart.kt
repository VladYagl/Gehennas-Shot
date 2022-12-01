package gehenna.factory

import gehenna.component.Floor
import gehenna.core.Entity
import gehenna.level.BasicLevel
import gehenna.utils.Point
import gehenna.utils.Size
import gehenna.utils.maxOf

interface LevelPart {
    fun spawnTo(to: Point, level: BasicLevel)
    fun needs(point: Point): Boolean
    val size: Size
}

class FixedPart(
        private val entities: List<Pair<Point, EntityConfig>>,
        private val factory: Factory<Entity>,
        private val addFloor: Boolean = false
) :
        LevelPart {
    override val size = Size(
            entities.maxOf { it.first.x },
            entities.maxOf { it.first.y }
    )

    private fun BasicLevel.spawn(config: EntityConfig, to: Point) {
        config.build(factory).forEach {
            spawn(it, to)
        }
    }


    override fun spawnTo(to: Point, level: BasicLevel) {
        entities.forEach { (point, config) ->
            level.spawn(config, to + point)
            if (addFloor && !level[to + point].any { it.has<Floor>() }) {
                level.spawn(factory.new("floor"), to + point) // TODO: Hardcoded floor is not good
            }
        }
    }

    override fun needs(point: Point) = entities.any { it.first.x == point.x && it.first.y == point.y }
}
