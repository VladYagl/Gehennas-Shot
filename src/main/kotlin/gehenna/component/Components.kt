package gehenna.component

import gehenna.core.Component
import gehenna.core.Entity
import gehenna.level.FovLevel
import gehenna.level.Level
import gehenna.utils.Dir
import gehenna.utils.Point
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

data class Health(
        override val entity: Entity,
        val max: Int
) : Component() {
    object Death : Entity.Event

    var current = max
//        private set //todo

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
    var tempMessage: String? = null

    fun add(text: String) {
        log.add(text)
        tempMessage = null
    }

    fun addTemp(text: String) {
        tempMessage = text
    }

    init {
        //todo: better but still need some filtering
        //subscribe<Senses.Sight.Saw> { if (it.entity.all<CharacterBehaviour>().isNotEmpty()) add("${it.entity} comes to view") }
    }
}

data class Stairs(override val entity: Entity, var destination: Pair<Level, Point>? = null) : Component()

data class Item(override val entity: Entity, val volume: Int) : Component()

data class Reflecting(override val entity: Entity) : Component()

data class Inventory(
        override val entity: Entity,
        val maxVolume: Int,
        private val items: ArrayList<Item> = ArrayList(),
        var gun: Item? = null
) : Component() {
    var currentVolume = items.sumBy { it.volume }
        private set

    fun add(item: Item): Boolean {
        if (item.volume + currentVolume > maxVolume) {
            return false
        }
        currentVolume += item.volume
        items.add(item)
        return true
    }

    fun remove(item: Item) {
        if (gun?.entity == item.entity) gun = null
        currentVolume -= item.volume
        items.remove(item)
    }

    fun equip(item: Item?) {
        gun = item
    }

    fun unequip() {
        gun = null
    }

    val contents
        get() = items.toList()

    init {
        gun?.let { add(it) }
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

data class Door(
        override val entity: Entity,
        val closedChar: Char,
        val openChar: Char,
        var closed: Boolean = true
) : Component() {
    private val char get() = if (closed) closedChar else openChar

    private val obstacle = Obstacle(entity, blockMove = closed, blockView = closed, blockPath = false)
    private val glyph = Glyph(entity, char = char, priority = 10)
    override val children: List<Component> = listOf(obstacle, glyph, Floor(entity))

    fun change(closed: Boolean) {
        obstacle.apply {
            blockMove = closed
            blockView = closed
            entity<Position>()?.update()
        }
        this.closed = closed
        glyph.apply { char = this@Door.char }
    }

    fun open() = change(true)
    fun close() = change(false)
}

data class DirectionalGlyph(override val entity: Entity, val glyphs: Map<Dir, Char>, val priority: Int = 10, val memorable: Boolean = false) : Component() {
    private val glyph = Glyph(entity, glyphs[Dir.zero] ?: '?', priority, memorable)
    override val children: List<Component> = listOf(glyph)

    init {
        subscribe<Entity.NewComponent<*>> {
            if (it.component is Position) {
                it.component.lastDir?.let { dir ->
                    glyph.char = glyphs[dir] ?: glyph.char
                }
            }
        }
    }
}

sealed class Senses : Component() {
    abstract fun visitFov(visitor: (Entity, Point) -> Unit)
    abstract fun isVisible(point: Point): Boolean

    data class Sight(override val entity: Entity, val range: Int) : Senses() {
        private var seen = HashMap<Entity, Long>()
        private var count = 0L
        private var fov: FovLevel.FovBoard? = null
        override fun visitFov(visitor: (Entity, Point) -> Unit) {
            val pos = entity<Position>()
            fov = pos?.level?.visitFov(pos, range) { target, point ->
                visitor(target, point)
                if (seen[target] != count) entity.emit(Saw(target))
                seen[target] = count + 1
            }
            count++
        }

        override fun isVisible(point: Point) = fov?.isVisible(point) ?: false

        data class Saw(val entity: Entity) : Entity.Event
    }

    data class TrueSight(override val entity: Entity) : Senses() {
        override fun isVisible(point: Point): Boolean = true

        override fun visitFov(visitor: (Entity, Point) -> Unit) {
            entity<Position>()?.let { pos ->
                for (point in pos.level.size.range) {
                    pos.level[point].forEach { entity -> visitor(entity, point) }
                }
            }
        }
    }

    data class Hearing(override val entity: Entity) : Senses() {
        override fun isVisible(point: Point): Boolean = false
        override fun visitFov(visitor: (Entity, Point) -> Unit) {} // TODO
    }
}
