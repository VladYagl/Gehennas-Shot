package gehenna.factory

import com.beust.klaxon.JsonReader
import gehenna.core.*
import gehenna.exceptions.*
import org.reflections.Reflections
import java.io.InputStream
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

//FIXME : THIS DEFINITELY NEEDS SOME TESTING !!!
//todo: make something similar to save manager
class EntityFactory : JsonFactory<Entity> {
    private val entities = HashMap<String, EntityBuilder>()

    private val reflections = Reflections("gehenna")
    val components = reflections.getSubTypesOf(Component::class.java).map { it.kotlin }
    private val mutators = reflections.getSubTypesOf(EntityMutator::class.java).map { it.kotlin }

    init {
        println("Creating EntityFactory...")
        println("\tcomponents: ${components.size}")
        println("\tmutators: ${mutators.size}")
    }

    private inner class ComponentBuilder(
            val constructor: KFunction<Component>,
            private val args: HashMap<KParameter, Any>
    ) {
        fun build(entity: Entity): Component {
            args[constructor.parameters[0]] = entity
            val buildArgs = args.mapValues { (parameter, value) ->
                buildValueFromType(parameter.type, value, this@EntityFactory)
            }
            return constructor.callBy(buildArgs)
        }
    }

    private inner class EntityBuilder(
            val components: List<ComponentBuilder>,
            val mutators: List<(Entity) -> Unit> = emptyList(),
            val customName: String? = null
    ) {
        fun build(name: String) = Entity(customName ?: name).also { entity ->
            components.forEach { builder ->
                entity.add(builder.build(entity))
            }
            mutators.forEach { mutator ->
                mutator(entity)
            }
        }
    }

    private fun JsonReader.nextComponent(componentName: String): ComponentBuilder {
        val constructor = components.firstOrNull {
            it.simpleName?.toLowerCase() == componentName.toLowerCase()
        }?.primaryConstructor ?: throw BadComponentException(componentName)
        return ComponentBuilder(constructor, nextArgs(constructor))
    }

    private fun JsonReader.nextMutator(mutatorName: String): (Entity) -> Unit {
        val constructor = mutators.firstOrNull {
            it.simpleName?.toLowerCase() == mutatorName.toLowerCase()
        }?.primaryConstructor ?: throw BadMutatorException(mutatorName)
        val args = nextArgs(constructor)
        return { entity ->
            val mutator = constructor.callBy(args.mapValues { (parameter, value) ->
                buildValueFromType(parameter.type, value, this@EntityFactory)
            })
            mutator.mutate(entity)
        }
    }

    private fun JsonReader.nextEntity(): EntityBuilder {
        val components = ArrayList<ComponentBuilder>()
        val mutators = ArrayList<(Entity) -> Unit>()
        var entityName: String? = null
        beginObject { name ->
            when (name) {
                "name" -> entityName = nextString()
                "super" -> {
                    val parentName = nextString()
                    entities[parentName]?.let { parent ->
                        components.addAll(parent.components)
                        mutators.addAll(parent.mutators)
                    } ?: throw UnknownSuperException(parentName)
                }
                "mutators" -> {
                    beginObject { mutatorName ->
                        mutators.add(nextMutator(mutatorName))
                    }
                }
                else -> {
                    val component = nextComponent(name)
                    components.removeAll { it.constructor == component.constructor }
                    components.add(component)
                }
            }
        }
        return EntityBuilder(components, mutators, entityName)
    }

    override fun loadJson(input: Pair<InputStream, String>) {
        val (stream, file) = input
        try {
            JsonReader(stream.reader()).use { reader ->
                reader.beginObject { name ->
                    try {
                        entities[name] = reader.nextEntity()
                    } catch (e: Throwable) {
                        throw ReadException(name, e)
                    }
                }
            }
        } catch (e: Throwable) {
            throw FactoryReadException(file, e)
        }
    }

    override fun new(name: String): Entity {
        return entities[name]?.build(name)?.apply { emit(Entity.Finish) } ?: throw NoSuchBuilderException(name)
    }
}

