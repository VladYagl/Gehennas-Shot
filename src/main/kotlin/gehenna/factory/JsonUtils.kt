package gehenna.factory

import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonReader
import gehenna.component.AmmoType
import gehenna.component.Item
import gehenna.core.Component
import gehenna.core.Faction
import gehenna.core.NamedFaction
import gehenna.core.SoloFaction
import gehenna.exceptions.NotAnItemException
import gehenna.exceptions.UnknownArgumentException
import gehenna.exceptions.UnknownTypeException
import gehenna.utils.Dice
import gehenna.utils.Dir
import gehenna.utils.toDice
import java.lang.Exception
import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.safeCast
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.jvmName

val projection = KTypeProjection.invariant(Item::class.createType())
val itemListType = ArrayList::class.createType(listOf(projection))
val itemType = Item::class.createType(nullable = true)
val itemTypeNoNull = Item::class.createType(nullable = false)

val componentProjection = KTypeProjection.covariant(Component::class.createType())
val componentType = KClass::class.createType(listOf(componentProjection), nullable = false)

fun JsonReader.beginObject(parser: (String) -> Unit) {
    beginObject {
        while (hasNext()) {
            parser(nextName())
        }
    }
}

fun JsonReader.beginArray(parser: (String) -> Unit) {
    beginArray {
        while (hasNext()) {
            parser(nextName())
        }
    }
}

fun JsonReader.nextStringList() = ArrayList<String>().also { list ->
    beginArray { name ->
        list.add(name)
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
        itemListType -> nextStringList()
        itemType -> nextString()
        itemTypeNoNull -> nextString()
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
        Faction::class.createType() -> { // todo: this needed to be managed by faction
            val name = value as String
            if (name == "solo") SoloFaction
            else NamedFaction(name)
        }
        itemListType -> {
            @Suppress("UNCHECKED_CAST")
            (value as List<String>).map {
                factory.new(it)<Item>() ?: throw NotAnItemException(it)
            }
        }
        itemType -> {
            factory.new(value as String)<Item>()
        }
        itemTypeNoNull -> {
            factory.new(value as String)<Item>()!!
        }
        componentType -> {
            //TODO: exposes factory.components
            factory.components.first { it.simpleName?.toLowerCase() == (value as String).toLowerCase() }
        }
        else -> if (type.jvmErasure.isSubclassOf(Enum::class)) {
            Class.forName(type.jvmErasure.jvmName).enumConstants.first {
                value == it.toString() // toString from enum returns it's name, so it should work
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

