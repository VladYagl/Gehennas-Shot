package gehenna.level

import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonReader
import gehenna.Entity
import gehenna.Factory
import gehenna.JsonFactory
import gehenna.utils.Point
import gehenna.utils.nextStringList
import java.io.InputStream

class LevelFactory(private val factory: Factory<Entity>) : JsonFactory<LevelPart> {
    private val levels = HashMap<String, LevelPart>()

    private data class LevelConfig(
            val rows: List<String>,
            val charMap: HashMap<Char, String>
    )

    private fun JsonReader.nextPart(): LevelPart {
        //fixme: this shit is messy
        val entities = ArrayList<Pair<Point, String>>()
        var charMapTemp: JsonObject? = null
        var rowsTemp: List<String>? = null
        beginObject {
            while (hasNext()) {
                when (nextName()) {
                    "entities" -> charMapTemp = nextObject()
                    "rows" -> rowsTemp = nextStringList()
                }
            }
        }
        val rows = rowsTemp!!
        val charMap = charMapTemp!!
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, char ->
                charMap.string(char.toString())?.let { name ->
                    entities.add((x to y) to name)
                }
            }
        }
        return FixedPart(entities, factory)
    }

    override fun loadJson(stream: InputStream) {
        JsonReader(stream.reader()).use { reader ->
            reader.beginObject {
                val name = reader.nextName()
                levels[name] = reader.nextPart()
            }
        }
    }

    override fun new(name: String) = levels[name] ?: throw Exception("no such part: $name")
}
