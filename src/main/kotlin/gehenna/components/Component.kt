package gehenna.components

import gehenna.Entity

abstract class Component {
    abstract val entity: Entity

    /** always after add */
    open fun onAdd() {}

    /** always after remove */
    open fun onRemove() {}
}