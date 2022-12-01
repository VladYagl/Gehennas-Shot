package gehenna.factory

import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonReader
import com.beust.klaxon.Klaxon
import com.beust.klaxon.token.LEFT_BRACE
import com.beust.klaxon.token.LEFT_BRACKET
import gehenna.component.Item
import gehenna.core.Component
import gehenna.core.Faction
import gehenna.core.toFaction
import gehenna.exception.UnknownArgumentException
import gehenna.exception.UnknownTypeException
import gehenna.utils.Dice
import gehenna.utils.Dir
import gehenna.utils.toDice
import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

private val itemTypeNoNull = Item::class.createType(nullable = false)
private val projection = KTypeProjection.invariant(itemTypeNoNull)
private val listProjection = KTypeProjection.invariant(List::class.createType(listOf(projection)))
private val itemListType = ArrayList::class.createType(listOf(projection))

private val itemBuilderTypeNoNull = Builder::class.createType(listOf(projection))
private val itemListBuilderType = Builder::class.createType(listOf(listProjection))

private val componentProjection = KTypeProjection.covariant(Component::class.createType())
private val componentType = KClass::class.createType(listOf(componentProjection), nullable = false)

val klaxon = Klaxon().converter(EntityConfig.ConfigConverter)

interface JsonConverter<T> {
    fun fromString(config: String): T
    fun fromArray(array: List<*>): T
    fun fromObject(obj: JsonObject): T
}

inline fun <reified T> JsonReader.next(converter: JsonConverter<T>? = null): T {
    lexer.nextToken()
    return when (lexer.peek()) {
        is LEFT_BRACE -> klaxon.parse<T>(this)!!
        is LEFT_BRACKET -> converter!!.fromArray(nextArray())
        else -> converter!!.fromString(nextString())
    }
}

fun JsonReader.beginObject(parser: (String) -> Unit) {
    beginObject {
        while (hasNext()) {
            parser(nextName())
        }
    }
}

fun JsonReader.nextValueFromType(type: KType): Any {
    return when (type) {
        Boolean::class.createType() -> nextBoolean()
        Double::class.createType() -> nextDouble()
        Int::class.createType() -> nextInt()
        Long::class.createType() -> nextLong()
        String::class.createType() -> nextString()
        Char::class.createType() -> nextInt().toChar()
        Faction::class.createType() -> nextString()
        Dice::class.createType() -> nextString()

        itemTypeNoNull -> next(EntityConfig.ConfigConverter)
        itemListType -> next(EntityConfig.ConfigConverter)
        itemBuilderTypeNoNull -> next(EntityConfig.ConfigConverter)
        itemListBuilderType -> next(EntityConfig.ConfigConverter)

        componentType -> nextString()
        else -> {
            if (type.jvmErasure.isSubclassOf(Enum::class)) {
                nextString()
            } else when (type.jvmErasure) {
                Map::class -> nextObject()
                else -> throw UnknownTypeException(type)
            }
        }
    }
}

fun <T> JsonReader.nextArgs(constructor: KFunction<T>): HashMap<KParameter, Any> {
    val args = HashMap<KParameter, Any>()
    beginObject { argName ->
        val parameter = constructor.parameters.firstOrNull {
            it.name == argName
        } ?: throw UnknownArgumentException(argName)
        val value: Any = nextValueFromType(parameter.type)
        args[parameter] = value
    }
    return args
}

fun buildValueFromType(type: KType, value: Any, factory: EntityFactory): Any? {
    return when (type) {
        Dice::class.createType() -> {
            (value as String).toDice()
        }
        Faction::class.createType() -> {
            (value as String).toFaction()
        }
        itemTypeNoNull -> {
            val list = (value as EntityConfig).build(factory)
            assert(list.size == 1)
            list.first().one<Item>()
        }
        itemListType -> {
            (value as EntityConfig).build(factory).map { it.one<Item>() }
        }
        itemBuilderTypeNoNull -> {
            Builder { (value as EntityConfig).build(factory).first().one<Item>() }
        }
        itemListBuilderType -> {
            Builder { (value as EntityConfig).build(factory).map { it.one<Item>() } }
        }
        componentType -> {
            factory.componentClassByName(value as String)
        }
        else -> if (type.jvmErasure.java.isEnum) {
            type.jvmErasure.java.enumConstants.first {
                value == it.toString() // toString from enum returns its name, so it should work
            }
        } else when (type.jvmErasure) {
            Map::class -> (value as JsonObject).map.mapKeys { (key, _) ->
                if (type.arguments[0].type == (Dir::class).createType()) {
                    Dir.firstOrNull { it.toString() == key } ?: Dir.zero
                } else {
                    key
                }
            }.mapValues { (_, value) ->
                if (type.arguments[1].type == (Char::class).createType()) {
                    (value as Int).toChar()
                } else {
                    value
                }
            }
            else -> value
        }
    }
}

