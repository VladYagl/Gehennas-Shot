open class Component(val entity: Entity) {
    init {
        entity.add(this)
    }
}

class Position(
    entity: Entity,
    val x: Int,
    val y: Int,
    val level: Level
) : Component(entity)

class Glyph(
    entity: Entity,
    val char: Char,
    val priority: Int = 0
) : Component(entity)

class Obstacle(
    entity: Entity,
    val blockMove: Boolean = false,
    val blockView: Boolean = false
) : Component(entity)

class Floor(entity: Entity) : Component(entity)

class Stats(
    entity: Entity,
    val speed: Int = 100
) : Component(entity)

abstract class WaitTime(entity: Entity, var time: Long = 0) : Component(entity)

