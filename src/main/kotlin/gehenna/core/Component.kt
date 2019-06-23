package gehenna.core

import java.io.Serializable

abstract class Component: Serializable {
    abstract val entity: Entity
    protected val handlers = ArrayList<(Entity.Event) -> Unit>()
    open val children: List<Component> = emptyList()

    fun onEvent(event: Entity.Event) = handlers.forEach { it(event) }

    protected inline fun <reified T : Entity.Event> subscribe(crossinline handler: (T) -> Unit) {
        handlers.add { event -> if (event is T) handler(event) }
    }

    init {
        //todo is it possible to not subscribe if children is empty?
        //todo dependencies through annotations maybe?
        subscribe<Entity.Add> {
            children.forEach {
                entity.add(it)
            }
        }
    }
}
