package gehenna.ui

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import asciiPanel.AsciiFont
import java.awt.Color
import java.awt.Color.lightGray
import java.io.InputStream

class Settings {
    val width = 1600
    val height = 850
    val logHeight = 8
    val infoWidth = 30

    val worldFont = AsciiFont("tilesets/Bisasam_16x16.png", 16, 16)
    val font = AsciiFont("tilesets/Curses_640x300diag.png", 8, 12)
    val backgroundColor = Color(32, 32, 32)
    val foregroundColor = lightGray!!
    val memoryColor = Color(96, 32, 32)

    fun toJson(): String {
        return klaxon.toJsonString(this)
    }
}

private object ColorConverter : Converter {
    override fun canConvert(cls: Class<*>) = cls == Color::class.java

    override fun toJson(value: Any): String = (value as Color).let {
        JsonObject(mapOf(
                "r" to it.red,
                "g" to it.green,
                "b" to it.blue
        )).toJsonString()
    }

    override fun fromJson(jv: JsonValue) = Color(jv.objInt("r"), jv.objInt("g"), jv.objInt("b"))
}

private object FontConverter : Converter {
    override fun canConvert(cls: Class<*>) = cls == AsciiFont::class.java

    override fun toJson(value: Any): String = (value as AsciiFont).let {
        JsonObject(mapOf(
                "r" to it.fontFilename,
                "g" to it.width,
                "b" to it.height
        )).toJsonString()
    }

    override fun fromJson(jv: JsonValue) = AsciiFont(jv.objString("file"), jv.objInt("width"), jv.objInt("height"))
}

private val klaxon = Klaxon().converter(ColorConverter).converter(FontConverter)

fun streamResource(name: String): Pair<InputStream, String> {
    //todo: Pair --- sucks
    return Pair((Thread::currentThread)().contextClassLoader.getResourceAsStream(name)!!, name)
}

fun loadSettings(input: Pair<InputStream, String>): Settings? {
    return klaxon.parse<Settings>(input.first)
}

fun main(args: Array<String>) {
    println(Settings().toJson())
}
