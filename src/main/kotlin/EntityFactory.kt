import com.beust.klaxon.JsonReader
import java.io.File
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.createType
import kotlin.reflect.full.primaryConstructor

//TODO : THIS DEFINITELY NEEDS SOME TESTING !!!
//TODO : MAKE SEPARATE CLASS FOR ENTITIES HASH MAP
//TODO : IT GETS HIS RESOURCES FROM MainFrame CLASS, I GUESS ITS NO GOOD
class EntityFactory {
    private val components = listOf(
        Position::class,
        Glyph::class,
        Obstacle::class,
        Floor::class,
        Stats::class,
        ThinkUntilSet::class,
        BulletBehaviour::class
    )

    private val entities = HashMap<String, List<Pair<KFunction<Component>, HashMap<KParameter, Any>>>>()

    fun newEntity(name: String): Entity {
        val list = entities[name]
        val entity = Entity(name)
        list!!.forEach { (constructor, args) ->
            args[constructor.parameters[0]] = entity
            val component = constructor.callBy(args)
            entity.add(component)
        }
        return entity
    }

    init {
        val url = MainFrame::class.java.classLoader.getResource("entities.json")
        println(url)
        val file = File(url.toURI())
        JsonReader(file.bufferedReader()).use { reader ->
            reader.beginObject {
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    val list = ArrayList<Pair<KFunction<Component>, HashMap<KParameter, Any>>>()
                    reader.beginObject {
                        while (reader.hasNext()) {
                            val componentName = reader.nextName()
                            val clazz = components.first {
                                it.simpleName?.toLowerCase() == componentName.toLowerCase()
                            }
                            val constructor = clazz.primaryConstructor!!
                            val args = HashMap<KParameter, Any>()
                            reader.beginObject {
                                while (reader.hasNext()) {
                                    val argName = reader.nextName()
                                    val parameter = constructor.parameters.first {
                                        it.name == argName
                                    }
                                    val value: Any = when (parameter.type) {
                                        Boolean::class.createType() -> reader.nextBoolean()
                                        Double::class.createType() -> reader.nextDouble()
                                        Int::class.createType() -> reader.nextInt()
                                        String::class.createType() -> reader.nextString()
                                        Char::class.createType() -> reader.nextInt().toChar()
                                        else -> throw Exception("Unkown type: " + parameter.type)
                                    }
                                    args[parameter] = value
                                }
                            }
                            list.add(Pair(constructor, args))
                        }
                    }
                    entities[name] = list
                }
            }
        }
    }
}
