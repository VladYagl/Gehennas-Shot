package gehenna.component

import gehenna.core.*

abstract class Consumable(val apply: (Entity, Context) -> Unit) : Component() {
    fun apply(target: Entity): Action {
        return object : Action() {
            override fun perform(context: Context): ActionResult {
                apply(target, context)
                entity.clean()
                return end()
            }
        }
    }
}

data class HealthPack(override val entity: Entity, val value: Int) : Consumable({ target, _ ->
    target<Health>()?.heal(value)
})

