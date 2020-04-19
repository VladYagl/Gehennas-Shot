package gehenna.component

import gehenna.action.Collide
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
        val range: Int = 20,
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
                    entity = bullet,
                    angle = random.nextAngle(angle, gun.spread),
                    speed = gun.speed + ammo.speed,
                    distance = ammo.range,
                    bounce = ammo.bounce,
                    waitTime = gun.delay,
                    collisionAction = { Collide(bullet, it, gun.damage + ammo.damage) }
            ).attach()
        }
    }
}
