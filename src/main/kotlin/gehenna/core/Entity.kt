package gehenna.core

import gehenna.exceptions.EntityMustHaveOneException
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
    data class NewComponent<T : Component>(val component: T) : Event

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

    inline fun <reified T : Component> one(): T {
        return if (T::class.isFinal) {
            components[T::class] as T? ?: throw EntityMustHaveOneException(name, T::class)
        } else {
            val children = components.mapNotNull {
                T::class.safeCast(it.value)
            }
            if (children.size == 1) children.first() // todo: concurrent shit this doesn't look perfect
            else throw EntityMustHaveOneException(name, T::class)
        }
    }

    inline fun <reified T : Component> has(): Boolean {
        return invoke<T>() != null
    }

    inline fun <reified T : Component> add(component: T) {
        //todo : add by T::class (to for example avoid need of any)
        components[component::class] = component
        component.onEvent(Add)
        emit(NewComponent<T>(component))
    }

    fun remove(component: Component) {
        component.onEvent(Remove)
        components.remove(component::class)
    }

    inline fun <reified T : Component> remove() {
        components[T::class]?.onEvent(Remove)
        components.remove(T::class)
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

