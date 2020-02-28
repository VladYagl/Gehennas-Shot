package gehenna.core

abstract class ActiveComponent : Component() {
    open var waitTime: Long = 0L
    abstract suspend fun action(): Action
    var lastResult: ActionResult? = null

    init {
        subscribe<Entity.Add> { ActionQueue.add(this) }
        subscribe<Entity.Remove> { ActionQueue.remove(this) }
    }
}

abstract class Effect : ActiveComponent() {
    open var duration: Long = 0
    open val endless: Boolean = false
}

