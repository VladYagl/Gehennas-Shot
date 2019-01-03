package gehenna.components

import gehenna.Entity
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.safeCast

//TODO DOES I REALLY NEED THIS SHIT
//TODO DO PROFILING AGAIN AND AFTER ZIRCON
object ComponentManager {
    private val components = HashMap<KClass<out Component>, HashSet<Entity>>()

    //Need this for tests, clever people sad that it's sign of code smell xd
    fun clear() {
        components.clear()
    }

    fun add(component: Component) {
        val set = components[component::class] ?: {
            val newSet = HashSet<Entity>()
            components[component::class] = newSet
            newSet
        }()
        set.add(component.entity)
    }

    fun remove(component: Component) {
        components[component::class]?.remove(component.entity)
    }

    fun <T : Component> all(clazz: KClass<T>): List<T> {
        return components.mapNotNull {
            if (clazz.isSuperclassOf(it.key))
                it.value.mapNotNull { entity -> clazz.safeCast(entity[it.key]) }
            else
                null
        }.flatten()
    }

    operator fun get(clazz: KClass<out Component>): List<Entity> {
        return components[clazz]?.toList() ?: emptyList()
    }

    operator fun get(vararg classes: KClass<out Component>): List<Entity> {
        var min = Int.MAX_VALUE
        var best: KClass<out Component> = classes[0]
        for (clazz in classes) {
            val size = components[clazz]?.size ?: 0
            if (size < min) {
                min = size
                best = clazz
            }
        }
        val list = ArrayList<Entity>()
        for (entity in components[best] ?: emptySet<Entity>()) {
            if (classes.all { entity.has(it) }) {
                list.add(entity)
            }
        }
        return list
    }
}