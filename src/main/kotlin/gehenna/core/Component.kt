package gehenna.core

abstract class Component {
    abstract val entity: Entity

    /** always after add */
    open fun onAdd() {}

    /** always after remove */
    open fun onRemove() {}
}