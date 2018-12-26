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
    operator fun plus(dir: Pair<Int, Int>): Pair<Int, Int> {
        return x + dir.first to y + dir.second
    }

    fun move(x: Int, y: Int) {
        level.move(entity, x, y)
    }

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
}

