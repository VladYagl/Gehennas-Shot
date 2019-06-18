package gehenna.core

import com.beust.klaxon.*
import gehenna.component.Position
import gehenna.level.DungeonLevelFactory
import gehenna.level.Level
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

class SaveManager(saveFileName: String) {

    data class SaveContext(
            val levels: List<DungeonLevelFactory.DungeonLevel>,
            val entities: List<Entity>
//            val components: List<Pair<KClass<out Component>, Component>>
    )

    private val saveFile = File(saveFileName)

    private val klaxon = Klaxon()
            .converter(object : Converter {
                //todo maybe there is a better way
                override fun canConvert(cls: Class<*>) = cls == Pair::class.java

                override fun fromJson(jv: JsonValue): Any? {
                    println("SHIT SHIT SHIT")
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun toJson(value: Any): String {
                    println("CUNT CUNT CUNT")
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
            })
            .converter(object : Converter {
                //todo maybe there is a better way
                override fun canConvert(cls: Class<*>) = cls.simpleName == "KClassImpl"

                override fun fromJson(jv: JsonValue): Any? {
                    println("SHIT SHIT SHIT")
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun toJson(value: Any) = "\"" + (value as KClass<*>).simpleName!!.toLowerCase() + "\""
            })
            .propertyStrategy(object : PropertyStrategy {
                // property does not have a backing field --> it's calculated, so dont save it
                // todo : filter startPositions from levels
                override fun accept(property: KProperty<*>): Boolean {
                    return property.javaField != null
                }
            })

    fun saveContext(context: Context) {
        //todo: better do something like with levels
        val saveContext = SaveContext(
                context.levels as List<DungeonLevelFactory.DungeonLevel>,
                context.levels.map { it.getAll() }.flatten()
//                context.levels.map { it.getAll().map { it.components.toList() } }.flatten().flatten()
        )

        saveFile.bufferedWriter().use {
            it.append(klaxon.toJsonString(saveContext))
        }
    }

    fun loadContext(context: Context) {
        val saveContext = klaxon.parse<SaveContext>(saveFile)
        println(saveContext)
    }
}