package gehenna.core

import gehenna.exception.EntityMustHaveOneException
import gehenna.utils.setVal
import java.io.Serializable
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.safeCast
import java.io.ObjectOutputStream
import java.io.ObjectInputStream


data class Entity(val name: String = "gehenna.core.Entity", val id: String = UUID.randomUUID().toString()) : Serializable {
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

    operator fun invoke(clazz: KClass<out Component>): Component? {
        return components[clazz]
    }

    inline fun <reified T: Any> all(): List<T> {
        return components.mapNotNull {
            T::class.safeCast(it.value)
        }
    }

    inline fun <reified T: Any> any(): T? {
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
            children.firstOrNull() ?: throw EntityMustHaveOneException(name, T::class)
        }
    }

    inline fun <reified T : Component> has(): Boolean {
        return invoke<T>() != null
    }

    fun has(clazz: KClass<out Component>): Boolean {
        return invoke(clazz) != null
    }

    inline fun <reified T : Component> add(component: T) {
        components[component::class] = component
        component.onEvent(Add)
        emit(NewComponent(component))
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

    private fun writeObject(outputStream: ObjectOutputStream) {
        outputStream.writeObject(name)
        outputStream.writeObject(id)
        outputStream.writeObject(components.values.toList())
    }

    private fun readObject(inputStream: ObjectInputStream) {
        this.setVal("name", inputStream.readObject())
        this.setVal("id", inputStream.readObject())
        this.setVal("components", HashMap<KClass<out Component>, Component>())
        val components = inputStream.readObject() as List<Component>
        components.forEach {
            this.components[it::class] = it
        }
    }
}

