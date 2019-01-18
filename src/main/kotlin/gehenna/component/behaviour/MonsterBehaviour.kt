package gehenna.component.behaviour

import com.beust.klaxon.internal.firstNotNullResult
import gehenna.action.Move
import gehenna.component.*
import gehenna.core.Action
import gehenna.core.Entity
import gehenna.utils.*
import java.lang.Math.abs

data class MonsterBehaviour(override val entity: Entity, override var waitTime: Long = 0) : Behaviour() {
    private var target: Position? = null
    private val dangerZone = HashSet<Point>()
    private val pos get() = entity<Position>()!!

    private fun updateSenses() {
        dangerZone.clear()
        val bullets = ArrayList<PredictableBehaviour>()
        entity.all<Senses>().forEach { sense ->
            sense.visitFov { obj, x, y ->
                if (obj != entity && obj<Obstacle>()?.blockMove == true) dangerZone.add(x to y)
                obj<BulletBehaviour>()?.let { bullets.add(it) }
                if (obj.name == "player") {
                    target = obj()
                }
            }
        }
        bullets.forEach { bullet ->
            dangerZone.addAll(pos.level.predict(bullet, entity<Stats>()?.speed?.toLong() ?: 100))
        }
    }

    private fun shoot(target: Position): Action? {
        return entity<Inventory>()?.all()
            ?.firstNotNullResult { it.entity.all<Gun>().firstOrNull() }
            ?.let { gun ->
                if (target == target.entity<Position>()) {
                    val diff = target.point - pos.point
                    if (diff.x == 0 || diff.y == 0 || abs(diff.x) == abs(diff.y)) {
                        gun.fire(entity, diff.dir)
                    } else null
                } else null
            }
    }

    private fun goto(target: Position): Action? {
        return pos.findPath(target.x, target.y)?.firstOrNull()?.let { next ->
            val dir = next.x - pos.x to next.y - pos.y
            if (next !in dangerZone) Move(entity, dir)
            else {
                val left = turnLeft(dir)
                val right = turnRight(dir)
                when {
                    pos + left !in dangerZone -> Move(entity, left)
                    pos + right !in dangerZone -> Move(entity, right)
                    else -> null
                }
            }
        }
    }

    private fun dodge(): Action? {
        return if (pos.point in dangerZone) {
            target?.point?.minus(pos.point)?.dir?.let { dir ->
                if (dir.plus(pos.point) !in dangerZone) Move(entity, dir) else null
            } ?: directions.firstOrNull { it + pos.point !in dangerZone }?.let { Move(entity, it) }
        } else null
    }

    private fun safeRandom(): Action? {
        val dirs = directions.plus(0 to 0).filter { pos.point + it !in dangerZone }
        return if (dirs.isNotEmpty()) Move(entity, dirs.random(random)) else null
    }

    private fun randomMove(): Action = safeRandom() ?: Move(entity, (random.nextInt(3) - 1) to (random.nextInt(3) - 1))

    override suspend fun action(): Action {
        if (lastResult?.succeeded == false) {
            return randomMove()
        }
        updateSenses()
        return dodge() ?: target?.let { shoot(it) ?: goto(it) ?: randomMove() } ?: randomMove()
    }
}