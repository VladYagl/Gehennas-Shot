package gehenna.factory

import com.beust.klaxon.JsonReader
import com.beust.klaxon.Klaxon
import gehenna.core.Entity
import gehenna.utils.Point
import gehenna.utils.at
import java.io.InputStream

class LevelPartFactory(private val factory: Factory<Entity>) : JsonFactory<LevelPart> {
    private val levels = HashMap<String, LevelPart>()
    private val klaxon = Klaxon()

    private data class LevelConfig(
            val rows: List<String>,
            val entities: Map<String, Any>
    ) {
        fun toPart(factory: Factory<Entity>): LevelPart {
            val list = ArrayList<Pair<Point, EntityConfig>>()
            rows.forEachIndexed { y, row ->
                row.forEachIndexed { x, char ->
                    entities[char.toString()]?.let { entity ->
                        when (entity) {
                            is String -> list.add((x at y) to EntityConfig.Name(entity))
                            is List<*> -> list.add((x at y) to EntityConfig.Choice(entity as List<String>))
                            else -> throw Exception("Unknown entity config: $entity")
                        }
                    }
                }
            }
            return FixedPart(list, factory)
        }
    }

    override fun loadJson(input: Pair<InputStream, String>) {
        val (stream, file) = input
        JsonReader(stream.reader()).use { reader ->
            reader.beginObject {
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    reader.lexer.nextToken() // consume ':'
                    klaxon.parse<LevelConfig>(reader)?.let { config ->
                        levels.put(name, config.toPart(factory))
                    }
                }
            }
        }
    }

    override fun new(name: String) = levels[name] ?: throw Exception("no such part: $name")
}
