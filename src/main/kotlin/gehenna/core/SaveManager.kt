package gehenna.core

import com.beust.klaxon.*
import gehenna.component.Inventory
import gehenna.level.Level
import org.reflections.Reflections
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

class SaveManager(saveFileName: String) {

    private val reflections = Reflections("gehenna.component")
    private val components = reflections.getSubTypesOf(Component::class.java).map { it.kotlin }

    data class SaveContext(
            val levels: List<Level>,
            val entities: List<Entity>,
            val components: List<ComponentDescriptor>
    )

    data class ComponentDescriptor(val value: Component) {
        val type: KClass<out Component> = value::class
//        val children: List<ComponentDescriptor> = value.children.map(::ComponentDescriptor)
    }

    private val saveFile = File(saveFileName)

    private val klaxon = Klaxon()

    init {
        val default = klaxon.findConverterFromClass(ComponentDescriptor::class.java, null)
        klaxon
                .converter(object : Converter {
                    //todo maybe there is a better way
                    override fun canConvert(cls: Class<*>) = cls.simpleName == "KClassImpl"

                    override fun fromJson(jv: JsonValue): Any? {
                        println("SHIT SHIT SHIT")
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun toJson(value: Any) = "\"" + (value as KClass<*>).simpleName + "\""
                })
                .converter(object : Converter {
                    override fun canConvert(cls: Class<*>) = cls == ComponentDescriptor::class.java

                    override fun fromJson(jv: JsonValue): Component {
                        val kClass = components.firstOrNull {
                            it.simpleName?.toLowerCase() == jv.objString("type").toLowerCase()
                        }!!
                        if (kClass.simpleName == "Inventory" || kClass.simpleName?.toLowerCase()?.contains("behaviour") == true) { // behaviours don't work cause of factions
                            return Inventory(Entity.world, 1)
                        }
                        println("Ready for: $kClass, {${jv.obj!!.obj("value")}} >>>")
                        val value = klaxon.fromJsonObject(jv.obj!!.obj("value")!!, kClass.java, kClass) as Component
                        println(value)
//                        val children = klaxon.parseFromJsonArray<Component>(jv.obj!!.array<JsonObject>("children")!!)
//                        println(children)
//                        value.children
                        return value
                    }

                    override fun toJson(value: Any) = default.toJson(value)
                })
                .converter(object : Converter {
                    override fun canConvert(cls: Class<*>) = cls == Char::class.java

                    override fun fromJson(jv: JsonValue): Char? {
                        return jv.int?.toChar()
                    }

                    override fun toJson(value: Any) = (value as Char).toInt().toString()
                })
                .propertyStrategy(object : PropertyStrategy {
                    // property does not have a backing field --> it's calculated, so dont save it
                    override fun accept(property: KProperty<*>): Boolean {
                        return property.javaField != null && property.name != "children"
                    }
                })
    }

    fun saveContext(context: Context) {
        //todo: better do something like with levels
        val saveContext = SaveContext(
                context.levels,
                context.levels.map { it.getAll() }.flatten(),
                context.levels
                        .map { it.getAll().map { it.components.values } }
                        .flatten()
                        .flatten()
                        .map(::ComponentDescriptor)
        )

        saveFile.bufferedWriter().use {
            it.append(klaxon.toJsonString(saveContext))
        }
    }

    fun loadContext(context: Context) {
        val saveContext = klaxon.parse<SaveContext>(saveFile)
        println("DONE!!")
    }
}