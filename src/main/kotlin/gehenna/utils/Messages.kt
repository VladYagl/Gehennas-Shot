@file:Suppress("ObjectPropertyName", "FunctionName")

package gehenna.utils

import gehenna.core.Entity
import java.awt.Color

const val _Actor = "%Actor%"
const val _actor = "%actor%"

const val _Actor_s = "%Actor_s%"
const val _actor_s = "%actor_s%"

fun Double.format(digits: Int): String = java.lang.String.format("%.${digits}f", this)

private val colorMap: Map<String, Color> = mapOf(
        //TODO: move this constants somewhere
        "dark_red" to Color(96, 32, 32),
        "warn" to Color(128, 32, 32),
        "boring" to Color(128, 128, 128),
        "love" to Color(128, 64, 128)
)

fun _fg(color: String, message: String): String {
    return "\${$color}$message\${normal}"
}

fun _bg(color: String, message: String): String {
    return "\$_{$color}$message\$_{normal}"
}

fun String.toColor() : Color {
    return colorMap[this] ?: throw Exception("Unknown color : '$this'")
}

fun String.prepareMessage(firstPerson: Boolean = false, actor: Entity = Entity.world, args: Map<String, String> = emptyMap()): String {
    return this
            .replace("\\[[^\\[\\]]+]".toRegex()) {
                if (firstPerson) {
                    ""
                } else {
                    it.value.drop(1).dropLast(1)
                }
            }
            .replace(_Actor.toRegex()) {
                if (firstPerson) {
                    "You"
                } else {
                    actor.name.capitalize()
                }
            }
            .replace(_actor.toRegex()) {
                if (firstPerson) {
                    "you"
                } else {
                    actor.name
                }
            }
            .replace(_Actor_s.toRegex()) {
                if (firstPerson) {
                    "Your"
                } else {
                    actor.name.capitalize() + "'s"
                }
            }
            .replace(_actor_s.toRegex()) {
                if (firstPerson) {
                    "your"
                } else {
                    actor.name + "'s"
                }
            }
            .replace("%[^%]+%".toRegex()) {
                args[it.value.drop(1).dropLast(1)] ?: it.value
            }
}
