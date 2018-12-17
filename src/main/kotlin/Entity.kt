import kotlin.reflect.KClass

enum class TEMP(val char: Char) {
    NONE('¿'),
    FLOOR('.'),
    WALL('▓'),
    PLAYER('@'),
}

class Entity {
    private val components = HashMap<KClass<out Component>, Component>()

    operator fun <T : Component> get(clazz: KClass<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return components[clazz] as T?
    }

    fun add(component: Component) {
        components[component::class] = component
        ComponentManager.add(component)
    }

    fun remove(component: Component) {
        components.remove(component::class)
        ComponentManager.remove(component)
    }
}

object ComponentManager {
    private val components = HashMap<KClass<out Component>, ArrayList<Component>>()

    fun add(component: Component) {
        val list = components[component::class] ?: {
            val newList = ArrayList<Component>()
            components[component::class] = newList
            newList
        }()
        list.add(component)
    }

    fun remove(component: Component) {
        components[component::class]?.remove(component)
    }

    operator fun <T : Component> get(clazz: KClass<T>): ArrayList<T> {
        @Suppress("UNCHECKED_CAST")
        return components[clazz] as ArrayList<T>
    }
}

open class Component(val entity: Entity) {
}

class Position(entity: Entity, val x: Int, val y: Int, val level: Level) : Component(entity)

class Glyph(entity: Entity, val char: Char) : Component(entity)
