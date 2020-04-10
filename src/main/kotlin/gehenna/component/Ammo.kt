package gehenna.component

import gehenna.component.behaviour.ProjectileBehaviour
import gehenna.core.Action.Companion.oneTurn
import gehenna.core.Component
import gehenna.core.Entity
import gehenna.ui.UIContext
import gehenna.utils.Dice
import gehenna.utils.Angle
import gehenna.utils.nextAngle
import gehenna.utils.random

@Suppress("EnumEntryName")
enum class AmmoType {
    c9mm, shell
}

data class Ammo(
        override val entity: Entity,

        val type: AmmoType,
//        val shootFunc: ShootFunc, // TODO: Find a way to combine them

        val damage: Dice,
        val speed: Int,
        val lifeTime: Long = 2 * oneTurn,
        val bounce: Boolean = false,

        private val volume: Int = 0
) : Component() {
    val item = Item(entity, volume, true)
    override val children: List<Component> = listOf(item)
}

abstract class ShootFunc : Component() {
    abstract operator fun invoke(pos: Position, angle: Angle, gun: Gun, ammo: Ammo, context: UIContext)
}

data class ProjectileSpawner(
        override val entity: Entity,
        private val projectileName: String,
        private val amount: Int = 1
) : ShootFunc() {
    override fun invoke(pos: Position, angle: Angle, gun: Gun, ammo: Ammo, context: UIContext) {
        repeat(amount) {
            val bullet = context.factory.new(projectileName)
            pos.spawnHere(bullet)
            ProjectileBehaviour(
                    bullet,
                    random.nextAngle(angle, gun.spread),
                    gun.damage + ammo.damage,
                    gun.speed + ammo.speed,
                    ammo.bounce,
                    gun.delay
            ).attach()
            DestroyTimer(bullet, ammo.lifeTime).attach()
        }
    }
}
