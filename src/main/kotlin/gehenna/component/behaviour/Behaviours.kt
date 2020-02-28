package gehenna.component.behaviour

import gehenna.action.Move
import gehenna.action.Think
import gehenna.core.*
import gehenna.core.Action.Companion.oneTurn
import gehenna.utils.Dir
import gehenna.utils.random

abstract class CharacterBehaviour : Behaviour() {
    open val faction: Faction = SoloFaction
}

data class RandomBehaviour(override val entity: Entity) : Behaviour() {
    override suspend fun behave() = Move(entity, Dir.random(random))
}

data class NoBehaviour(override val entity: Entity) : CharacterBehaviour() {
    override suspend fun behave() = Think(oneTurn * 100)
}
