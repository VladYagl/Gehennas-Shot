package gehenna.core

import gehenna.component.ActiveComponent
import gehenna.component.behaviour.PredictableBehaviour
import java.util.*

private val active = ArrayList<ActiveComponent>()

//TODO I DONT WANT THIS STATIC OBJECT
object ActionQueue : List<ActiveComponent> by active {
    private val predictables = TreeSet<PredictableBehaviour>(compareBy { it.entity.id })

    fun update() {
        active.sortWith(compareBy({ it.waitTime }, { it.hashCode() }))
    }

    fun add(actor: ActiveComponent) {
        if (actor is PredictableBehaviour) {
            predictables.add(actor)
        }
        active.add(actor)
    }

    fun remove(actor: ActiveComponent) {
        if (actor is PredictableBehaviour) {
            predictables.remove(actor)
        }
        active.remove(actor)
    }

    fun predictables(): Set<PredictableBehaviour> {
        return predictables
    }
}