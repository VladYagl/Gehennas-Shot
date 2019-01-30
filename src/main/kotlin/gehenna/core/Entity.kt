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

    inline fun <reified T : Component> any(): T? {
        return components.mapNotNull {
            T::class.safeCast(it.value)
        }.firstOrNull()
    }

    inline fun <reified T : Component> one(): T? {
        val children = components.mapNotNull {
            T::class.safeCast(it.value)
        }
        if (children.size == 1) return children.first()
        else throw Exception("Expected that entity $name should have only one component of type ${T::class}")
    }

    inline fun <reified T : Component> has(): Boolean {
        return invoke<T>() != null
    }

    fun add(component: Component) {
        components[component::class] = component
        component.onEvent(Add)
    }

    fun remove(component: Component) {
        components.remove(component::class)
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

