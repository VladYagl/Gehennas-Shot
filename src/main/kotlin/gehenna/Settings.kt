package gehenna

import asciiPanel.AsciiFont
import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import java.awt.Color
import java.io.InputStream

class Settings {
    val width = 1600
    val height = 850
    val logHeight = 8
    val infoWidth = 30

    val worldFont = AsciiFont("tilesets/Bisasam_16x16.png", 16, 16)
    val font = AsciiFont("tilesets/Curses_640x300diag.png", 8, 12)
    val backgroundColor = Color(32, 32, 32)

    val drawEachUpdate = true
    val updateStep = 25

    fun toJson(): String {
        return klaxon.toJsonString(this)
    }
}

private val colorConverter = object : Converter {
    override fun canConvert(cls: Class<*>) = cls == Color::class.java

    override fun toJson(value: Any): String = (value as Color).let {
        """
        {
            "r" : ${it.red},
            "g" : ${it.green},
            "b" : ${it.blue}
        }
        """.trimIndent()
    }

    override fun fromJson(jv: JsonValue) = Color(jv.objInt("r"), jv.objInt("g"), jv.objInt("b"))

}

private val fontConverter = object : Converter {
    override fun canConvert(cls: Class<*>) = cls == AsciiFont::class.java

    override fun toJson(value: Any): String = (value as AsciiFont).let {
        """
        {
            "file" : "${it.fontFilename}",
            "width" : ${it.width},
            "height" : ${it.height}
        }
        """.trimIndent()
    }

    override fun fromJson(jv: JsonValue) = AsciiFont(jv.objString("file"), jv.objInt("width"), jv.objInt("height"))

}

private val klaxon = Klaxon().converter(colorConverter).converter(fontConverter)

fun streamResource(name: String): InputStream {
    return (Thread::currentThread)().contextClassLoader.getResourceAsStream(name)!!
}

fun loadSettings(stream: InputStream): Settings? {
    return klaxon.parse<Settings>(stream)
}

fun main(args: Array<String>) {
    println(Settings().toJson())
}
