package gehenna.components

import gehenna.Entity
import gehenna.Level
import gehenna.utils.*

abstract class Component {
    abstract val entity: Entity

    /** always after add */
    open fun onAdd() {}

    /** always after remove */
    open fun onRemove() {}
}

data class Position(
    override val entity: Entity,
    val x: Int,
    val y: Int,
    val level: Level
) : Component() {
    val point: Pair<Int, Int>
        get() {
            return x to y
        }

    operator fun plus(dir: Pair<Int, Int>): Pair<Int, Int> {
        return x + dir.x to y + dir.y
    }

    fun move(x: Int, y: Int) {
        level.move(entity, x, y)
    }

    val neighbors: HashSet<Entity> get() = level[x, y]

    override fun onRemove() {
        level.remove(this)
    }

    override fun onAdd() {
        level.spawn(this)
    }
}

data class Glyph(
    override val entity: Entity,
    val char: Char,
    val priority: Int = 0,
    val memorable: Boolean = true
) : Component()

data class Obstacle(
    override val entity: Entity,
    val blockMove: Boolean = false,
    val blockView: Boolean = false,
    val blockPath: Boolean = blockMove
) : Component()

data class Floor(override val entity: Entity) : Component()

data class Stats(
    override val entity: Entity,
    val speed: Int = 100
) : Component()

abstract class WaitTime(open var time: Long = 0) : Component()

data class Health(
    override val entity: Entity,
    val max: Int
) : Component() {
    var current = max
        private set

    fun dealDamage(amount: Int) {
        current -= amount
        if (current <= 0)
            entity.clean() // FIXME: If player dies his logger is cleared too
    }
}

data class Logger(override val entity: Entity) : Component() {
    val log = ArrayList<String>()
    fun add(text: String) {
        log.add(text)
    }
}

data class Stairs(override val entity: Entity, var pos: Position? = null) : Component()
