import com.beust.klaxon.JsonReader
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import javax.swing.JOptionPane.ERROR_MESSAGE
import javax.swing.JOptionPane.showMessageDialog
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.createType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmName
import org.reflections.util.FilterBuilder
import org.reflections.util.ClasspathHelper
import org.reflections.scanners.ResourcesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import org.reflections.util.ConfigurationBuilder
import java.util.LinkedList


//TODO : THIS DEFINITELY NEEDS SOME TESTING !!!
//TODO : MAKE SEPARATE CLASS FOR ENTITIES HASH MAP
class EntityFactory {
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
        val reflections = Reflections(
            ConfigurationBuilder()
                .addUrls(ClasspathHelper.forJavaClassPath())
                .setScanners(SubTypesScanner())
        )
        val components = reflections.getSubTypesOf(Component::class.java).map { it.kotlin }

        val stream = (Thread::currentThread)().contextClassLoader.getResourceAsStream("entities.json")
        JsonReader(stream.reader()).use { reader ->
            reader.beginObject {
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    val list = ArrayList<Pair<KFunction<Component>, HashMap<KParameter, Any>>>()
                    reader.beginObject {
                        while (reader.hasNext()) {
                            val componentName = reader.nextName()
                            val clazz = components.firstOrNull {
                                it.simpleName?.toLowerCase() == componentName.toLowerCase()
                            }
                                ?: throw Exception("In entities.json: Entity [$name] contains unknown component [$componentName]")
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
                            list.add(constructor to args)
                        }
                    }
                    entities[name] = list
                }
            }
        }
    }
}
