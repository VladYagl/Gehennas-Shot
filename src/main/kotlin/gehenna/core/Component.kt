package gehenna.core

import java.io.Serializable

abstract class Component : Serializable {
    abstract val entity: Entity
    protected val handlers = ArrayList<(Entity.Event) -> Unit>()
    open val children: List<Component> = emptyList()
    var attached: Boolean = false
        private set

    fun onEvent(event: Entity.Event) = handlers.forEach { it(event) }

    protected inline fun <reified T : Entity.Event> subscribe(crossinline handler: (T) -> Unit) {
        handlers.add { event -> if (event is T) handler(event) }
    }

    init {
        //todo is it possible to not subscribe if children is empty?
        //todo dependencies through annotations maybe?
        subscribe<Entity.Add> {
            attached = true
            children.forEach {
                entity.add(it)
            }
        }
        subscribe<Entity.Remove> {
            attached = false
        }
    }
}
