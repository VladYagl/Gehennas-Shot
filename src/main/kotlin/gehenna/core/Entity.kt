package gehenna.core

import gehenna.factory.Factory
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.safeCast

//FIXME : CANT HAVE SAME COMPONENT TYPE TWICE
data class Entity(val name: String = "gehenna.core.Entity", val factory: Factory<Entity>, val id: String = UUID.randomUUID().toString()) {
    private val components = HashMap<KClass<out Component>, Component>()

    interface Event
    object Add : Event
    object Remove : Event
    object Finish : Event

    fun emit(event: Event) {
        components.values.toList().forEach { it.onEvent(event) }
    }

    operator fun <T : Component> get(clazz: KClass<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return components[clazz] as T?
    }

    fun <T : Component> all(clazz: KClass<T>): List<T> {
        return components.mapNotNull {
            clazz.safeCast(it.value)
        }
    }

    fun <T : Component> has(clazz: KClass<T>): Boolean {
        return get(clazz) != null
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
}

