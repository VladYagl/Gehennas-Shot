package gehenna.core

import gehenna.component.ActiveComponent
import gehenna.component.behaviour.PredictableBehaviour
import java.util.*

private val active: Queue<ActiveComponent> = PriorityQueue(compareBy({ it.waitTime }, { it.entity.id }))

//TODO I DONT WANT THIS STATIC OBJECT
object ActionQueue : Queue<ActiveComponent> by active {
    private val predictables = TreeSet<PredictableBehaviour>(compareBy { it.entity.id })

    override fun add(element: ActiveComponent): Boolean {
        if (element is PredictableBehaviour) {
            predictables.add(element)
        }
        return active.add(element)
    }

    override fun remove(element: ActiveComponent): Boolean {
        if (element is PredictableBehaviour) {
            predictables.remove(element)
        }
        return active.remove(element)
    }

    fun predictables(): Set<PredictableBehaviour> {
        return predictables
    }
}