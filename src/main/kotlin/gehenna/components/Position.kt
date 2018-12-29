package gehenna.components

import gehenna.Entity
import gehenna.Level
import gehenna.utils.x
import gehenna.utils.y

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

    val neighbors: List<Entity> get() = level[x, y].filter { it != entity }

    override fun onRemove() {
        level.remove(this)
    }

    override fun onAdd() {
        level.spawn(this)
    }
}