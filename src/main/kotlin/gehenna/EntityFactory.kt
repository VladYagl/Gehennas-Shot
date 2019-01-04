package gehenna

import com.beust.klaxon.JsonReader
import gehenna.components.Component
import gehenna.components.Item
import org.reflections.Reflections
import java.io.InputStream
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.primaryConstructor


interface EntityFactory {
    fun newEntity(name: String): Entity
}

//FIXME : THIS DEFINITELY NEEDS SOME TESTING !!!
//TODO: Throw proper exceptions of where error happens and why
class JsonFactory : EntityFactory {
    private val entities = HashMap<String, EntityBuilder>()

    private val reflections = Reflections("gehenna.components")
    private val components = reflections.getSubTypesOf(Component::class.java).map { it.kotlin }
    private val projection = KTypeProjection.invariant(Item::class.createType())
    private val itemListType = ArrayList::class.createType(listOf(projection))

    private inner class ComponentBuilder(private val constructor: KFunction<Component>, private val args: HashMap<KParameter, Any>) {
        fun build(entity: Entity): Component {
            args[constructor.parameters[0]] = entity
            return constructor.callBy(
                    args.mapValues { (parameter, value) ->
                        if (parameter.type == itemListType) {
                            @Suppress("UNCHECKED_CAST")
                            (value as List<String>).map {
                                newEntity(it)[Item::class] ?: throw Exception("$it is not an Item")
                            }
                        } else {
                            value
                        }
                    }
            )
        }
    }

    private inner class EntityBuilder(private val components: List<ComponentBuilder>) {
        fun build(name: String): Entity {
            val entity = Entity(name, this@JsonFactory)
            components.forEach { builder ->
                entity.add(builder.build(entity))
            }
            return entity
        }
    }

    private fun JsonReader.nextStringList(): List<String> {
        val list = ArrayList<String>()
        beginArray {
            while (hasNext()) {
                list.add(nextString())
            }
        }
        return list
    }

    private fun JsonReader.nextComponent(): ComponentBuilder {
        val componentName = nextName()
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
        beginObject {
            while (hasNext()) {
                list.add(nextComponent())
            }
        }
        return EntityBuilder(list)
    }

    fun loadJson(stream: InputStream) {
        JsonReader(stream.reader()).use { reader ->
            reader.beginObject {
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    entities[name] = reader.nextEntity()
                }
            }
        }
    }

    override fun newEntity(name: String): Entity {
        return entities[name]?.build(name) ?: throw Exception("no such entity: $name")
    }
}

