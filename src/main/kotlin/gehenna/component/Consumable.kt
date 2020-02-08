package gehenna.component

import gehenna.core.*
import gehenna.core.Action.Companion.oneTurn

abstract class Consumable(val time: Long, val apply: (Entity, Context) -> Unit) : Component() {
    fun apply(target: Entity): Action {
        return object : Action(time) {
            override fun perform(context: Context): ActionResult {
                apply(target, context)
                entity.clean()
                return end()
            }
        }
    }
}

data class HealthPack(override val entity: Entity, val value: Int) : Consumable(oneTurn, { target, _ ->
    target<Health>()?.heal(value)
})

