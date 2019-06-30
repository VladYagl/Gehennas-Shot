package gehenna.utils

import gehenna.core.Entity

const val _Actor = "%Actor%"
const val _actor = "%actor%"

fun Double.format(digits: Int): String = java.lang.String.format("%.${digits}f", this)

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
            .replace("%[^%]+%".toRegex()) {
                args[it.value.drop(1).dropLast(1)] ?: it.value
            }
}
