package gehenna.components

import gehenna.Entity
import gehenna.level.FovLevel
import gehenna.utils.until

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

abstract class WaitTime(open var time: Long = 0) : Component() // TODO: let it manage ComponentManager??

data class Health(
        override val entity: Entity,
        val max: Int
) : Component() {
    var current = max
        private set

    fun dealDamage(amount: Int) {
        current -= amount
        if (current <= 0) {
            entity[Inventory::class]?.dropAll()
            entity.clean() // FIXME: If player dies his logger is cleared too
        }
    }
}

data class Logger(override val entity: Entity) : Component() {
    val log = ArrayList<String>()
    fun add(text: String) {
        log.add(text)
    }
}

data class Stairs(override val entity: Entity, var pos: Position? = null) : Component()

data class Item(override val entity: Entity, val volume: Int) : Component()

data class Inventory(
        override val entity: Entity,
        val maxVolume: Int,
        private val items: ArrayList<Item> = ArrayList()
) : Component() {
    private var currentVolume = 0

    fun add(item: Item): Boolean {
        if (item.volume + currentVolume > maxVolume) {
            return false
        }
        currentVolume += item.volume
        items.add(item)
        return true
    }

    fun remove(item: Item) {
        currentVolume -= item.volume
        items.remove(item)
    }

    fun all(): List<Item> {
        return items.toList()
    }

    fun dropAll() {
        entity[Position::class]?.let { pos ->
            items.forEach { item ->
                pos.level.spawn(item.entity, pos.x, pos.y)
            }
        }
    }
}

sealed class Senses : Component() {
    abstract fun visitFov(visitor: (Entity, Int, Int) -> Unit)
    abstract fun isVisible(x: Int, y: Int): Boolean

    data class Sight(override val entity: Entity, val range: Int) : Senses() {
        private var fov: FovLevel.FovBoard? = null
        override fun visitFov(visitor: (Entity, Int, Int) -> Unit) {
            val pos = entity[Position::class]
            fov = pos?.level?.visitFov(pos.x, pos.y, range, visitor)
        }

        override fun isVisible(x: Int, y: Int) = fov?.isVisible(x, y) ?: false
    }

    data class TrueSight(override val entity: Entity) : Senses() {
        override fun isVisible(x: Int, y: Int): Boolean = true

        override fun visitFov(visitor: (Entity, Int, Int) -> Unit) {
            entity[Position::class]?.let { pos ->
                for ((x, y) in (0 to 0) until (pos.level.width to pos.level.height)) {
                    pos.level[x, y].forEach { entity -> visitor(entity, x, y) }
                }
            }
        }
    }

    data class Hearing(override val entity: Entity) : Senses() {
        override fun isVisible(x: Int, y: Int): Boolean = false
        override fun visitFov(visitor: (Entity, Int, Int) -> Unit) {} // TODO
    }
}
