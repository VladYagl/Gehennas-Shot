package gehenna.core

import java.io.Serializable

interface Faction: Serializable {
    fun isEnemy(other: Faction): Boolean
    fun isFriend(other: Faction): Boolean
}

data class NamedFaction(val name: String) : Faction {
    override fun isEnemy(other: Faction): Boolean {
        return this != other
    }

    override fun isFriend(other: Faction): Boolean {
        return this == other
    }
}

object SoloFaction : Faction {
    override fun isEnemy(other: Faction): Boolean = true

    override fun isFriend(other: Faction): Boolean = false
}

fun String.toFaction(): Faction {
    return if (this == "solo") SoloFaction
    else NamedFaction(this)
}
