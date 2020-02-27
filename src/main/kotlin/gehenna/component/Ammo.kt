package gehenna.component

import gehenna.core.Action.Companion.oneTurn
import gehenna.core.Component
import gehenna.core.Entity
import gehenna.utils.Dice

@Suppress("EnumEntryName")
enum class AmmoType {
    c9mm, shell
}

data class Ammo(
        override val entity: Entity,

        val type: AmmoType,
        val projectileName: String,

        val damage: Dice,
        val speed: Int,
        val lifeTime: Long = 3 * oneTurn,
        val bounce: Boolean = false,

        private val volume: Int = 0
) : Component() {
    val item = Item(entity, volume, true)
    override val children: List<Component> = listOf(item)
}
