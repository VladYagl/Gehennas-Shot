package gehenna.factory

import gehenna.component.Floor
import gehenna.core.Entity
import gehenna.level.BasicLevel
import gehenna.utils.*

interface LevelPart {
    fun spawnTo(to: Point, level: BasicLevel)
    fun needs(point: Point): Boolean
    val size: Size
}

sealed class EntityConfig {
    data class Name(val name: String) : EntityConfig()
    data class Multiple(val list: List<EntityConfig>) : EntityConfig()
    data class Choice(val list: List<Pair<EntityConfig, Double>>) : EntityConfig()
}

class FixedPart(
        private val entities: List<Pair<Point, EntityConfig>>,
        private val factory: Factory<Entity>,
        private val addFloor: Boolean = false
) :
        LevelPart {
    override val size = Size(
            entities.maxOf { it.first.x } ?: 0,
            entities.maxOf { it.first.y } ?: 0
    )

    private fun BasicLevel.spawn(config: EntityConfig, to: Point) {
        when (config) {
            is EntityConfig.Name -> spawn(factory.new(config.name), to)
            is EntityConfig.Choice -> spawn(config.list.random(), to)
            is EntityConfig.Multiple -> config.list.forEach { spawn(it, to) }
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
