package gehenna.core

abstract class Component {
    abstract val entity: Entity

    open fun onEvent(event: Entity.Event) {}
}