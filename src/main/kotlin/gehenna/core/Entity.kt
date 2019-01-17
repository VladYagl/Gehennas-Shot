package gehenna.core

import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.safeCast

//FIXME : CANT HAVE SAME COMPONENT TYPE TWICE
data class Entity(val name: String = "gehenna.core.Entity", val id: String = UUID.randomUUID().toString()) {
    val components = HashMap<KClass<out Component>, Component>()

    interface Event
    object Add : Event
    object Remove : Event
    object Finish : Event

    fun emit(event: Event) {
        components.values.toList().forEach { it.onEvent(event) }
    }

    inline operator fun <reified T : Component> invoke(): T? {
        return components[T::class] as T?
    }

    inline fun <reified T : Component> all(): List<T> {
        return components.mapNotNull {
            T::class.safeCast(it.value)
        }
    }

    inline fun <reified T : Component> has(): Boolean {
        return invoke<T>() != null
    }

    fun add(component: Component) {
        components[component::class] = component
        ComponentManager.add(component)
        component.onEvent(Add)
    }

    fun remove(component: Component) {
        components.remove(component::class)
        ComponentManager.remove(component)
        component.onEvent(Remove)
    }

    fun clean() {
        components.toList().forEach {
            remove(it.second)
        }
    }

    override fun toString(): String {
        return name
    }

    companion object {
        val world = Entity("World")
    }
}

