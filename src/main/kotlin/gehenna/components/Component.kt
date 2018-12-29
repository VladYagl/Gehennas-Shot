package gehenna.components

import gehenna.Entity

abstract class Component {
    abstract val entity: Entity

    /** always after add */
    open fun onAdd() {}

    /** always after remove */
    open fun onRemove() {}
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
}

data class Gun(override val entity: Entity, val bullet: String = "bullet") : Component()
