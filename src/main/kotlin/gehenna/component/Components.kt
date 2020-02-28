package gehenna.component

import gehenna.core.Action
import gehenna.core.Component
import gehenna.core.Entity
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

data class Reflecting(override val entity: Entity) : Component()

data class Flying(override val entity: Entity) : Component()

data class MeleeWeapon(override val entity: Entity, val damage: Dice) : Component()

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

data class DirectionalGlyph(
        override val entity: Entity,
        val glyphs: Map<Dir, Char>,
        val priority: Int = 10,
        val memorable: Boolean = false
) : Component() {
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

