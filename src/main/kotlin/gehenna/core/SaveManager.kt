package gehenna.core

import com.beust.klaxon.*
import gehenna.component.Inventory
import gehenna.component.Reflecting
import gehenna.level.Level
import org.reflections.Reflections
import java.io.File
import java.lang.Exception
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

class SaveManager(saveFileName: String) {

    data class SaveData(
            val levels: MutableList<Level>,
            val entities: List<Entity>,
            val components: List<ComponentDescriptor>)

    data class ComponentDescriptor(val value: Component) {
        val type: KClass<out Component> = value::class
    }

    private inline fun <reified T> fromConverter(crossinline from: (JsonValue) -> T) = object : Converter {
        override fun canConvert(cls: Class<*>) = cls == T::class.java

        override fun fromJson(jv: JsonValue): T = from(jv)

        override fun toJson(value: Any) = klaxon.toJsonString(value)
    }

    private val reflections = Reflections("gehenna.component")
    private val components = reflections.getSubTypesOf(Component::class.java).map { it.kotlin }

    private val saveFile = File(saveFileName)

    private val propertyStrategy = object : PropertyStrategy {
        // property does not have a backing field --> it's calculated, so dont save it
        override fun accept(property: KProperty<*>): Boolean {
            if (property.name == "items") {
                println("SHIt")
            }
            return property.javaField != null && property.name != "children"
        }
    }

    private val charConverter = object : Converter {
        override fun canConvert(cls: Class<*>) = cls == Char::class.java

        override fun fromJson(jv: JsonValue): Char? {
            return jv.int?.toChar()
        }

        override fun toJson(value: Any) = (value as Char).toInt().toString()
    }

    private val typeConverter = object : Converter {
        //todo maybe there is a better way
        override fun canConvert(cls: Class<*>) = cls.simpleName == "KClassImpl"

        override fun fromJson(jv: JsonValue): KClass<out Component>? {
            return fromString(jv.string)
        }

        fun fromString(name: String?): KClass<out Component>? {
            return components.firstOrNull {
                it.simpleName?.toLowerCase() == name?.toLowerCase()
            }
        }

        override fun toJson(value: Any) = "\"" + (value as KClass<*>).simpleName + "\""
    }

    private val klaxon = Klaxon()
            .propertyStrategy(propertyStrategy)
            .converter(charConverter)
            .converter(typeConverter)

    fun saveContext(context: Context) {
        //todo: better do something like with levels for entities and maybe components
        val saveData = SaveData(
                context.levels,
                context.levels.map { it.getAll() }.flatten(),
                context.levels
                        .map { it.getAll().map { it.components.values } }
                        .flatten()
                        .flatten()
                        .map(::ComponentDescriptor)
        )

        saveFile.bufferedWriter().use {
            it.append(klaxon.toJsonString(saveData))
        }
    }

    fun loadContext() {
        val saveData = klaxon.parseJsonObject(saveFile.bufferedReader())
        val levels = klaxon.parseFromJsonArray<Level>(saveData.array<JsonObject>("levels")!!)!!
        val entities = klaxon.parseFromJsonArray<Entity>(saveData.array<JsonObject>("entities")!!)!!
        println(saveData)
        println(levels)
        println(entities)

        val klax = Klaxon()
        klax.propertyStrategy(propertyStrategy)
                .converter(charConverter)
                .converter(typeConverter)
                .converter(fromConverter { jv -> levels.first { it.id == jv.objString("id") } })
                .converter(fromConverter { jv -> entities.first { it.id == jv.objString("id") } })
                .converter(fromConverter { jv ->
                    val kClass = typeConverter.fromString(jv.obj?.string("type"))!!
                    if (kClass.simpleName == "Inventory" || kClass.simpleName?.toLowerCase()?.contains("behaviour") == true) { // behaviours don't work cause of factions
                        ComponentDescriptor(Reflecting(Entity.world))
                    } else {
                        println("Ready for: $kClass, {${jv.obj!!.obj("value")}} >>>")
                        val value = klax.fromJsonObject(jv.obj!!.obj("value")!!, kClass.java, kClass) as Component
                        println(value)
                        ComponentDescriptor(value)
                    }
                })
        val components = klax.parseFromJsonArray<ComponentDescriptor>(saveData.array<JsonObject>("components")!!)!!

        components.forEach { it.value.entity.add(it.value) }

        println("DONE!!")
    }
}