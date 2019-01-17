package gehenna.factory

import com.beust.klaxon.JsonReader
import gehenna.component.Item
import gehenna.core.Component
import gehenna.core.Entity
import gehenna.utils.nextStringList
import org.reflections.Reflections
import java.io.InputStream
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.primaryConstructor

//FIXME : THIS DEFINITELY NEEDS SOME TESTING !!!
//TODO: Throw proper exceptions of where error happens and why
class EntityFactory : JsonFactory<Entity> {
    private val entities = HashMap<String, EntityBuilder>()

    private val reflections = Reflections("gehenna.component")
    private val components = reflections.getSubTypesOf(Component::class.java).map { it.kotlin }
    private val projection = KTypeProjection.invariant(Item::class.createType())
    private val itemListType = ArrayList::class.createType(listOf(projection))

    private inner class ComponentBuilder(
        private val constructor: KFunction<Component>,
        private val args: HashMap<KParameter, Any>
    ) {
        fun build(entity: Entity): Component {
            args[constructor.parameters[0]] = entity
            return constructor.callBy(
                args.mapValues { (parameter, value) ->
                    if (parameter.type == itemListType) {
                        @Suppress("UNCHECKED_CAST")
                        (value as List<String>).map {
                            new(it)<Item>() ?: throw Exception("$it is not an Item")
                        }
                    } else {
                        value
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
        val clazz = components.firstOrNull {
            it.simpleName?.toLowerCase() == componentName.toLowerCase()
        } ?: throw Exception("In entities.json: contains unknown component [$componentName]")
        val constructor = clazz.primaryConstructor ?: throw Exception("class $clazz don't have suitable constructor")
        val args = HashMap<KParameter, Any>()
        beginObject {
            while (hasNext()) {
                val argName = nextName()
                val parameter = constructor.parameters.first {
                    it.name == argName
                }
                val value: Any = when (parameter.type) {
                    Boolean::class.createType() -> nextBoolean()
                    Double::class.createType() -> nextDouble()
                    Int::class.createType() -> nextInt()
                    Long::class.createType() -> nextLong()
                    String::class.createType() -> nextString()
                    Char::class.createType() -> nextInt().toChar()
                    itemListType -> nextStringList()
                    else -> throw Exception("Unkown type: " + parameter.type)
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
                            entities[parent]?.components
                                ?: throw Exception("No supper entity: $parent")
                        )
                    }
                    else -> list.add(nextComponent(name))
                }
            }
        }
        return EntityBuilder(list, entityName)
    }

    override fun loadJson(stream: InputStream) {
        JsonReader(stream.reader()).use { reader ->
            reader.beginObject {
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    entities[name] = reader.nextEntity()
                }
            }
        }
    }

    override fun new(name: String): Entity {
        return entities[name]?.build(name)?.also { it.emit(Entity.Finish) } ?: throw Exception("no such entity: $name")
    }
}

