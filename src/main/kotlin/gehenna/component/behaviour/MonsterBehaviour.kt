package gehenna.component.behaviour

import gehenna.action.*
import gehenna.component.*
import gehenna.core.Action
import gehenna.core.Action.Companion.oneTurn
import gehenna.core.Entity
import gehenna.core.Faction
import gehenna.core.PredictableBehaviour
import gehenna.utils.*

data class MonsterBehaviour(
        override val entity: Entity,
        override val faction: Faction,
        override var waitTime: Long = random.nextLong(100),
        override val speed: Int = 100) : CharacterBehaviour() {
    var target: Position? = null
    private val dangerZone = HashSet<Point>()
    private val pos get() = entity.one<Position>()

    private fun updateSenses() {
        dangerZone.clear()
        val bullets = ArrayList<PredictableBehaviour<Any>>()
        var newTarget: Position? = null
        entity.all<Senses>().forEach { sense ->
            sense.visitFov { obj, point ->
                if (obj != entity && obj<Obstacle>()?.blockMove == true) dangerZone.add(point)
                obj.any<PredictableBehaviour<Any>>()?.let { bullets.add(it) }
                obj.any<CharacterBehaviour>()?.let { behaviour ->
                    if (faction.isEnemy(behaviour.faction)
                            && obj != entity
                            && newTarget?.let { (it - pos).max > (point - pos).max } != false)
                        newTarget = obj()
                }
            }
        }
        newTarget?.let { target = it }
        bullets.forEach { bullet ->
            dangerZone.addAll(pos.level.predict(bullet, scaleTime(oneTurn, speed)))
        }
    }

    private fun shoot(target: Position): Action? {
        return entity<MainHandSlot>()?.gun?.let { gun ->

            if (gun.magazine.isEmpty()) {
                val newAmmo = entity<Inventory>()?.items?.mapNotNull {
                    val ammo = it.entity<Ammo>()
                    if (ammo?.type == gun.ammoType) {
                        ammo
                    } else {
                        null
                    }
                }
                return if (newAmmo != null && newAmmo.isNotEmpty()) {
                    gun.load(entity, newAmmo)
                } else {
                    null
                }
            }

            if (target == target.entity<Position>()) {
                val diff = target - pos
                val tempDir = LineDir(diff.x, diff.y)

                tempDir.findBestError(pos)?.let { error ->
                    return gun.fire(entity, LineDir(diff.x, diff.y, error))
                }

                return null
            } else null
        }
    }

    private fun pickup(): Action? { //todo find items and go to them
        return entity<Position>()?.neighbors?.mapNotNull { it<Item>() }?.let { items ->
            if (items.isNotEmpty()) {
                Pickup(entity, items)
            } else {
                null
            }
        }
    }

    private fun goto(target: Position): Action? {
        return pos.findPath(target)?.firstOrNull()?.let { next ->
            val dir = next.x - pos.x on next.y - pos.y
            if (next !in dangerZone) Move(entity, dir)
            else {
                val left = dir.turnLeft
                val right = dir.turnRight
                when {
                    pos + left !in dangerZone -> Move(entity, left)
                    pos + right !in dangerZone -> Move(entity, right)
                    else -> null
                }
            }
        }
    }

    private fun attack(target: Position): Action? {
        return if (target == target.entity<Position>() && (target - pos).max == 1) {
            Attack(entity, (target - pos).dir)
        } else null
    }

    private fun dodge(): Action? {
        return if (pos.x at pos.y in dangerZone) {
            target?.minus(pos)?.dir?.let { dir ->
                if (dir.plus(pos) !in dangerZone) Move(entity, dir) else null
            } ?: Dir.firstOrNull { it + pos !in dangerZone }?.let { Move(entity, it) }
        } else null
    }

    private fun safeRandom(): Action? {
        val dirs = Dir.plus(0 on 0).filter { pos + it !in dangerZone }
        return if (dirs.isNotEmpty()) Move(entity, dirs.random(random)) else null
    }

    private fun randomMove(): Action = safeRandom() ?: Move(entity, Dir.random(random))

    //    private fun wait(): Action = Wait
    private fun wait(): Action = Think(25)

    override suspend fun behave(): Action {
        if (lastResult?.succeeded == false) {
            return randomMove()
        }
        updateSenses()
//        return dodge() ?: target?.let { shoot(it) ?: pickup() ?: goto(it) ?: randomMove() } ?: pickup() ?: randomMove()
        return dodge() ?: target?.let { shoot(it) ?: pickup() ?: attack(it) ?: goto(it) ?: randomMove() } ?: pickup()
        ?: wait()
    }
}