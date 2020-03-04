package gehenna.factory

import com.beust.klaxon.*
import gehenna.core.Entity
import gehenna.exception.*
import gehenna.utils.Point
import gehenna.utils.at
import java.io.InputStream

class LevelPartFactory(private val factory: Factory<Entity>) : JsonFactory<LevelPart> {
    private val levels = HashMap<String, LevelPart>()

    private data class LevelConfig(
            val rows: List<String>,
            val entities: Map<String, EntityConfig>,
            val addFloor: Boolean = false
    ) {
        fun toPart(factory: Factory<Entity>): LevelPart {
            val list = ArrayList<Pair<Point, EntityConfig>>()
            rows.forEachIndexed { y, row ->
                row.forEachIndexed { x, char ->
                    entities[char.toString()]?.let { entity ->
                        list.add((x at y) to entity)
                    }
                }
            }
            return FixedPart(list, factory, addFloor)
        }
    }

    override fun loadJson(input: Pair<InputStream, String>) {
        val (stream, file) = input
        try {
            JsonReader(stream.reader()).use { reader ->
                reader.beginObject { name ->
                    try {
                        levels[name] = reader.next<LevelConfig>().toPart(factory)
                    } catch (e: Exception) {
                        throw ReadException(name, e)
                    }
                }
            }
        } catch (e: Throwable) {
            throw FactoryReadException(file, e)
        }
    }

    override fun new(name: String) = levels[name] ?: throw NoSuchBuilderException(name)
}
