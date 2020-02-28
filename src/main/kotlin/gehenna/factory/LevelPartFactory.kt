package gehenna.factory

import com.beust.klaxon.*
import gehenna.core.Entity
import gehenna.exception.*
import gehenna.utils.Point
import gehenna.utils.at
import java.io.InputStream

class LevelPartFactory(private val factory: Factory<Entity>) : JsonFactory<LevelPart> {
    private val levels = HashMap<String, LevelPart>()
    private val klaxon = Klaxon().converter(ConfigConverter)

    private object ConfigConverter : Converter {
        //For some reason for map arguments are always java.lang.Class
        override fun canConvert(cls: Class<*>) = cls == java.lang.Class::class.java

        private fun fromString(config: String): EntityConfig {
            val split = config.split(" ")
            return if (split.size == 1) {
                EntityConfig.Name(config)
            } else {
                val (name, count) = split
                EntityConfig.Multiple((1..count.toInt()).map { EntityConfig.Name(name) })
            }
        }

        private fun fromArray(array: JsonArray<*>): EntityConfig {
            return EntityConfig.Multiple(array.map { fromAny(it!!) })
        }

        private fun fromObject(obj: JsonObject): EntityConfig {
            obj.array<String>("multiple")?.let { return fromArray(it) }

            obj["choice"]?.let {
                return when (it) {
                    is JsonArray<*> -> {
                        EntityConfig.Choice(it.map { config -> fromAny(config!!) to (1.0 / it.size) })
                    }
                    is JsonObject -> {
                        val list = it.map.map { (name, chance) ->
                            require(chance is Double) { "Chance to spawn should be Double: name = $name, chance = $chance" }
                            fromString(name) to chance
                        }
                        EntityConfig.Choice(list)
                    }
                    else -> throw GehennaException("Bed choice config: $it")
                }
            }

            throw GehennaException("Can create config from JsonObject: $obj")
        }

        private fun fromAny(value: Any): EntityConfig {
            return when (value) {
                is String -> fromString(value)
                is JsonArray<*> -> fromArray(value)
                is JsonObject -> fromObject(value)
                else -> throw GehennaException("Can't create config from: $value")
            }
        }

        override fun fromJson(jv: JsonValue): EntityConfig {
            return fromAny(jv.inside ?: throw Exception("Json Value inside is null: $jv"))
        }

        override fun toJson(value: Any): String {
            throw NotImplementedError()
        }

    }

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
                    reader.lexer.nextToken() // consume ':'
                    try {
                        klaxon.parse<LevelConfig>(reader)?.let { config ->
                            levels.put(name, config.toPart(factory))
                        }
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
