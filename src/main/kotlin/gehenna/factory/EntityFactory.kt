package gehenna.factory

import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonReader
import gehenna.component.Item
import gehenna.core.Component
import gehenna.core.Entity
import gehenna.exceptions.*
import gehenna.utils.Dir
import gehenna.utils.nextStringList
import org.reflections.Reflections
import java.io.InputStream
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

//FIXME : THIS DEFINITELY NEEDS SOME TESTING !!!
//TODO: Throw proper exceptions of where error happens and why
class EntityFactory : JsonFactory<Entity> {
    private val entities = HashMap<String, EntityBuilder>()

    private val reflections = Reflections("gehenna.component")
    private val components = reflections.getSubTypesOf(Component::class.java).map { it.kotlin }
    private val projection = KTypeProjection.invariant(Item::class.createType())
    private val itemListType = ArrayList::class.createType(listOf(projection))
    private val itemType = Item::class.createType(nullable = true)

    private inner class ComponentBuilder(
            private val constructor: KFunction<Component>,
            private val args: HashMap<KParameter, Any>
    ) {
        fun build(entity: Entity): Component {
            args[constructor.parameters[0]] = entity
            return constructor.callBy(
                    args.mapValues { (parameter, value) ->
                        when (parameter.type) {
                            itemListType -> {
                                @Suppress("UNCHECKED_CAST")
                                (value as List<String>).map {
                                    new(it)<Item>() ?: throw NotAnItemException(it)
                                }
                            }
                            itemType -> {
                                new(value as String)<Item>()
                            }
                            else -> {
                                when (parameter.type.jvmErasure) {
                                    Map::class -> (value as JsonObject).map.mapKeys { (key, _) ->
                                        if (parameter.type.arguments[0].type == (Dir::class).createType()) {
                                            Dir.firstOrNull { it.toString() == key } ?: Dir.zero
                                        } else {
                                            key
                                        }
                                    }.mapValues { (_, value) ->
                                        if (parameter.type.arguments[1].type == (Char::class).createType()) {
                                            (value as Int).toChar()
                                        } else {
                                            value
                                        }
                                    }
                                    else -> value
                                }
                            }
                        }
                    }
            )
        }
    }

    private inner class EntityBuilder(val components: List<ComponentBuilder>, val customName: String? = null) {
        fun build(name: String) = Entity(customName ?: name).also { entity ->
            components.forEach { builder ->
                entity.add(builder.build(entity))
            }
        }
    }

    private fun JsonReader.nextComponent(componentName: String): ComponentBuilder {
        val constructor = components.firstOrNull {
            it.simpleName?.toLowerCase() == componentName.toLowerCase()
        }?.primaryConstructor ?: throw BadComponentException(componentName)
        val args = HashMap<KParameter, Any>()
        beginObject {
            while (hasNext()) {
                val argName = nextName()
                val parameter = constructor.parameters.firstOrNull {
                    it.name == argName
                } ?: throw UnknownArgumentException(argName)
                val value: Any = when (parameter.type) {
                    Boolean::class.createType() -> nextBoolean()
                    Double::class.createType() -> nextDouble()
                    Int::class.createType() -> nextInt()
                    Long::class.createType() -> nextLong()
                    String::class.createType() -> nextString()
                    Char::class.createType() -> nextInt().toChar()
                    itemListType -> nextStringList()
                    itemType -> nextString()
                    else -> {
                        when (parameter.type.jvmErasure) {
                            Map::class -> nextObject()
                            else -> throw UnknownTypeException(parameter.type)
                        }
                    }
                }
                args[parameter] = value
            }
        }
        return ComponentBuilder(constructor, args)
    }

    private fun JsonReader.nextEntity(): EntityBuilder {
        val list = ArrayList<ComponentBuilder>()
        var entityName: String? = null
        beginObject {
            while (hasNext()) {
                val name = nextName()
                when (name) {
                    "name" -> entityName = nextString()
                    "super" -> {
                        val parent = nextString()
                        list.addAll(
                                entities[parent]?.components ?: throw UnknownSuperException(parent)
                        )
                    }
                    else -> list.add(nextComponent(name))
                }
            }
        }
        return EntityBuilder(list, entityName)
    }

    override fun loadJson(input: Pair<InputStream, String>) {
        val (stream, file) = input
        try {
            JsonReader(stream.reader()).use { reader ->
                reader.beginObject {
                    while (reader.hasNext()) {
                        val name = reader.nextName()
                        try {
                            entities[name] = reader.nextEntity()
                        } catch (e: Throwable) {
                            throw EntityReadException(name, e)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            throw FactoryReadException(file, e)
        }
    }

    override fun new(name: String): Entity {
        return entities[name]?.build(name)?.also { it.emit(Entity.Finish) } ?: throw NoSuchEntityException(name)
    }
}

