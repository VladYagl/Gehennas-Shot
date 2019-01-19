package gehenna.component

import gehenna.core.Component
import gehenna.core.Entity
import gehenna.level.FovLevel
import gehenna.level.Level
import gehenna.utils.Point
import gehenna.utils.at
import gehenna.utils.random

data class Glyph(
    override val entity: Entity,
    var char: Char,
    val priority: Int = 0,
    val memorable: Boolean = true
) : Component()

data class Obstacle(
    override val entity: Entity,
    var blockMove: Boolean = false,
    var blockView: Boolean = false,
    var blockPath: Boolean = blockMove
) : Component()

data class Floor(override val entity: Entity) : Component()

data class Stats(
    override val entity: Entity,
    val speed: Int = 100
) : Component()

data class Health(
    override val entity: Entity,
    val max: Int
) : Component() {
    object Death : Entity.Event

    var current = max
        private set

    fun dealDamage(amount: Int) {
        current -= amount
        if (current <= 0) {
            entity.emit(Death)
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

data class Stairs(override val entity: Entity, var destination: Pair<Level, Point>? = null) : Component()

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

    fun all() = items.toList()

    init {
        subscribe<Health.Death> {
            entity<Position>()?.let { pos ->
                items.forEach { item ->
                    pos.spawnHere(item.entity)
                }
            }
        }
    }
}

data class ChooseOneItem(
    override val entity: Entity,
    private val items: ArrayList<Item> = ArrayList()
) : Component() {
    init {
        subscribe<Entity.Finish> {
            entity<Inventory>()?.add(items.random(random))
            entity.remove(this)
        }
    }
}

data class Door(override val entity: Entity, var closed: Boolean = true) : Component() {
    //todo -> it can add glyph and obstacle if there is no
    fun change(closed: Boolean) {
        entity<Obstacle>()?.apply {
            blockMove = closed
            blockView = closed
            entity<Position>()?.update()
        }
        entity<Glyph>()?.apply { char = (if (closed) '+' else 254.toChar()) } // todo
        this.closed = closed
    }

    fun open() = change(true)
    fun close() = change(false)
}

sealed class Senses : Component() {
    abstract fun visitFov(visitor: (Entity, Int, Int) -> Unit)
    abstract fun isVisible(x: Int, y: Int): Boolean

    data class Sight(override val entity: Entity, val range: Int) : Senses() {
        private var fov: FovLevel.FovBoard? = null
        override fun visitFov(visitor: (Entity, Int, Int) -> Unit) {
            val pos = entity<Position>()
            fov = pos?.level?.visitFov(pos.x, pos.y, range, visitor)
        }

        override fun isVisible(x: Int, y: Int) = fov?.isVisible(x, y) ?: false
    }

    data class TrueSight(override val entity: Entity) : Senses() {
        override fun isVisible(x: Int, y: Int): Boolean = true

        override fun visitFov(visitor: (Entity, Int, Int) -> Unit) {
            entity<Position>()?.let { pos ->
                for ((x, y) in (0 at 0) until (pos.level.width at pos.level.height)) {
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
