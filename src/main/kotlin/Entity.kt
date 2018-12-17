import kotlin.reflect.KClass
import kotlin.reflect.full.safeCast

class Entity(val name: String = "Entity") {
    private val components = HashMap<KClass<out Component>, Component>()

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
    }

    fun remove(component: Component) {
        components.remove(component::class)
        ComponentManager.remove(component)
    }

    override fun toString(): String {
        return "Entity($name)"
    }
}

