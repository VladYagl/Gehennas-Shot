package gehenna.component

import gehenna.core.*
import gehenna.core.Action.Companion.oneTurn

abstract class Consumable(val time: Long, val apply: (Entity, Context) -> Unit) : Component() {
    fun apply(target: Entity): Action {
        return SimpleAction(time) { context ->
            apply(target, context)
            entity.clean()
        }
    }
}

data class HealthPack(override val entity: Entity, val value: Int) : Consumable(oneTurn, { target, _ ->
    target<Health>()?.heal(value)
})

