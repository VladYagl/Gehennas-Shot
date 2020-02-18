package gehenna.component

import gehenna.core.Action.Companion.oneTurn
import gehenna.core.Component
import gehenna.core.Entity
import gehenna.utils.Dice

@Suppress("EnumEntryName")
enum class AmmoType {
    bullet9mm, shell
}

data class Ammo(
        override val entity: Entity,

        val type: AmmoType,
        val projectileName: String,

        val damage: Dice,
        val speed: Int,
        val lifeTime: Long = 3 * oneTurn,
        val bounce: Boolean = false,

        val capacity: Int,
        var amount: Int = capacity
) : Component() {

}
