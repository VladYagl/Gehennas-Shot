package gehenna.component

import gehenna.core.*
import gehenna.level.FovLevel
import gehenna.level.Level
import gehenna.utils.*
import kotlin.math.min

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
        private set

    fun dealDamage(amount: Int, action: Action) {
        current -= amount
        if (current <= 0) {
            action.logFor(entity, _fg("warn", "$_Actor is killed!"))
            entity.emit(Death)
            entity.clean() // FIXME: If player dies his logger is cleared too
        }
    }

    fun heal(amount: Int) {
        current += amount
        current = min(current, max)
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

data class Item(override val entity: Entity, val volume: Int) : Component() {
    var inventory: Inventory? = null
    var slot: Slot? = null

    /**
     * Unequips items if it is in a slot, others does nothing
     */
    fun unequip() {
        val slot = slot
        assert(slot == null || slot.item == this)
        slot?.unequip()
    }

    init {
        subscribe<Entity.Remove> {
            unequip()
            inventory?.remove(this)
        }
    }
}

data class Reflecting(override val entity: Entity) : Component()

data class Flying(override val entity: Entity) : Component()

interface Slot {
    var item: Item?

    fun equip(newItem: Item) {
        assert(item == null) { "Trying to equip $newItem in busy slot: $this" }
        assert(isValid(newItem))
        item = newItem
        newItem.slot = this
    }

    fun unequip() {
        item?.slot = null
        item = null
    }

    fun isValid(item: Item): Boolean = true
}

data class MainHandSlot(override val entity: Entity, override var item: Item? = null) : Component(), Slot {
    val gun: Gun? get() = item?.entity?.invoke()

    val damage: Dice
        get() {
            val item = item
            return if (item == null) {
                "d3".toDice() // TODO: Fist damage
            } else {
                item.entity<MeleeWeapon>()?.damage ?: Dice.SingleDice(item.volume / 5)
            }
        }
}

data class MeleeWeapon(override val entity: Entity, val damage: Dice) : Component()

data class Inventory(
        override val entity: Entity,
        val maxVolume: Int,
        val items: ArrayList<Item> = ArrayList()
) : Component() {
    var currentVolume = items.sumBy { it.volume }
        private set

    fun add(item: Item): Boolean {
        if (item.volume + currentVolume > maxVolume) {
            return false
        }
        currentVolume += item.volume
        items.add(item)
        item.inventory = this
        return true
    }

    fun remove(item: Item) {
        item.unequip()
        currentVolume -= item.volume
        items.remove(item)
        item.inventory = null
    }

    val contents
        get() = items.toList()

    init {
        //todo: Do you need death? You can call it on entity.remove?
        subscribe<Health.Death> {
            entity<Position>()?.let { pos ->
                items.forEach { item ->
                    pos.spawnHere(item.entity)
                }
            }
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
    override val children: List<Component> = listOf(obstacle, glyph)

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

    fun update(dir: Dir) {
        glyph.char = glyphs[dir] ?: glyph.char
    }

    init {
        subscribe<Entity.NewComponent<*>> {
            (it.component as? Position)?.lastDir?.let { dir -> update(dir) }
        }
    }
}

sealed class Senses : Component() {
    abstract fun visitFov(visitor: (Entity, Point) -> Unit)
    abstract fun isVisible(point: Point): Boolean

    data class Sight(override val entity: Entity, val range: Int) : Senses() {
        private var seen = HashMap<Entity, Long>()
        private var count = 0L
        @Transient
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
        //todo: is visible should return true in radius, so you can add stuff like hearing gun shots
        override fun isVisible(point: Point): Boolean = false

        override fun visitFov(visitor: (Entity, Point) -> Unit) {} // TODO
    }
}
