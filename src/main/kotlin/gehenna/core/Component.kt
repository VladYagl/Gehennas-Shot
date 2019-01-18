package gehenna.core

abstract class Component {
    abstract val entity: Entity
    protected val handlers = ArrayList<(Entity.Event) -> Unit>()

    fun onEvent(event: Entity.Event) = handlers.forEach { it(event) }

    protected inline fun <reified T : Entity.Event> subscribe(crossinline handler: (T) -> Unit) {
        handlers.add { event -> if (event is T) handler(event) }
    }
}
