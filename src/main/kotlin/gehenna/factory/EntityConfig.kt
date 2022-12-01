package gehenna.factory

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonValue
import gehenna.core.Entity
import gehenna.exception.GehennaException
import gehenna.utils.random

sealed class EntityConfig {
    data class Name(val name: String) : EntityConfig()
    data class Multiple(val list: List<EntityConfig>) : EntityConfig()
    data class Choice(val list: List<Pair<EntityConfig, Double>>) : EntityConfig()

    fun build(factory: Factory<Entity>): List<Entity> {
        return when (this) {
            is Name -> listOf(factory.new(this.name))
            is Choice -> this.list.random().build(factory)
            is Multiple -> this.list.map { it.build(factory) }.flatten()
        }
    }

    object ConfigConverter : Converter, JsonConverter<EntityConfig> {
        //For some reason for map arguments are always java.lang.Class
        override fun canConvert(cls: Class<*>) = (cls == java.lang.Class::class.java) || (cls == EntityConfig::class.java)

        override fun fromString(config: String): EntityConfig {
            val split = config.split(" ")
            return if (split.size == 1) {
                Name(config)
            } else {
                val (name, count) = split
                Multiple((1..count.toInt()).map { Name(name) })
            }
        }

        override fun fromArray(array: List<*>): EntityConfig {
            return Multiple(array.map { fromAny(it!!) })
        }

        override fun fromObject(obj: JsonObject): EntityConfig {
            obj.array<String>("multiple")?.let { return fromArray(it) }

            obj["choice"]?.let {
                return when (it) {
                    is JsonArray<*> -> {
                        Choice(it.map { config -> fromAny(config!!) to (1.0 / it.size) })
                    }
                    is JsonObject -> {
                        val list = it.map.map { (name, chance) ->
                            require(chance is Double) { "Chance to spawn should be Double: name = $name, chance = $chance" }
                            fromString(name) to chance
                        }
                        Choice(list)
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
}