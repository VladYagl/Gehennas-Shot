import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.safeCast

object ComponentManager {
    private val components = HashMap<KClass<out Component>, ArrayList<Entity>>()

    //Need this for tests, clever people sad that it's sign of code smell xd
    fun clear() {
        components.clear()
    }

    fun add(component: Component) {
        val list = components[component::class] ?: {
            val newList = ArrayList<Entity>()
            components[component::class] = newList
            newList
        }()
        list.add(component.entity)
    }

    fun remove(component: Component) {
        components[component::class]?.remove(component.entity)
    }

    fun <T : Component> all(clazz: KClass<T>): List<T> {
        return components.mapNotNull {
            if (it.key.isSubclassOf(clazz))
                it.value.mapNotNull { entity -> clazz.safeCast(entity[it.key]) }
            else
                null
        }.flatten()
    }

    operator fun get(clazz: KClass<out Component>): List<Entity> {
        return components[clazz] ?: emptyList()
    }

    operator fun get(vararg classes: KClass<out Component>): List<Entity> {
        var min = Int.MAX_VALUE
        var best: ArrayList<Entity> = ArrayList()
        for (clazz in classes) {
            val list = components[clazz] ?: return emptyList()
            if (list.size < min) {
                min = list.size
                best = list
            }
        }
        val list = ArrayList<Entity>()
        for (entity in best) {
            if (classes.all { entity.has(it) }) {
                list.add(entity)
            }
        }
        return list
    }
}