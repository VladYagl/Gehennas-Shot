package gehenna.core

import gehenna.component.WaitTime
import gehenna.component.behaviour.PredictableBehaviour
import java.util.*

//TODO DOES I REALLY NEED THIS SHIT
//TODO DO PROFILING AGAIN AND AFTER ZIRCON
object ComponentManager {
    private val waiters = ArrayList<WaitTime>()
    private val predictables = TreeSet<PredictableBehaviour>()

    fun update() {
        waiters.sortWith(compareBy({ it.time }, { it.entity.id }))
    }

    fun add(component: Component) {
        when (component) {
            is WaitTime -> waiters.add(component)
            is PredictableBehaviour -> predictables.add(component)
        }
    }

    fun remove(component: Component) {
        when (component) {
            is WaitTime -> waiters.remove(component)
            is PredictableBehaviour -> predictables.remove(component)
        }
    }

    fun waiters(): List<WaitTime> {
        return waiters.toList()
    }

    fun predictables(): Set<PredictableBehaviour> {
        return predictables
    }
}