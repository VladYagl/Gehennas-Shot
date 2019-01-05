package gehenna.core

import kotlin.reflect.KClass

abstract class Component {
    abstract val entity: Entity
    protected open val handlers: Map<KClass<out Entity.Event>, (Entity.Event) -> Unit> = emptyMap()

    open fun onEvent(event: Entity.Event) {}
}