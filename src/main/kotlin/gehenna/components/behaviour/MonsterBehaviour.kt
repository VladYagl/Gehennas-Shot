package gehenna.components.behaviour

import com.beust.klaxon.internal.firstNotNullResult
import gehenna.Entity
import gehenna.actions.Action
import gehenna.actions.Move
import gehenna.components.*
import gehenna.utils.*
import java.lang.Math.abs

data class MonsterBehaviour(override val entity: Entity, override var time: Long) : Behaviour() {
    private var target: Position? = null
    private val dangerZone = HashSet<Point>()
    private val pos get() = entity[Position::class]!!

    private fun updateSenses() {
        dangerZone.clear()
        val bullets = ArrayList<PredictableBehaviour>()
        entity.all(Senses::class).forEach { sense ->
            sense.visitFov { obj, x, y ->
                if (obj != entity && obj[Obstacle::class]?.blockMove == true) dangerZone.add(x to y)
                obj[BulletBehaviour::class]?.let { bullets.add(it) }
                if (obj.name == "player") {
                    target = obj[Position::class]
                }
            }
        }
        bullets.forEach { bullet ->
            dangerZone.addAll(pos.level.predict(bullet, entity[Stats::class]?.speed?.toLong() ?: 100))
        }
    }

    private fun shoot(target: Position): Action? {
        return entity[Inventory::class]?.all()
                ?.firstNotNullResult { it.entity.all(Gun::class).firstOrNull() }
                ?.let { gun ->
                    if (target == target.entity[Position::class]) {
                        val diff = target.point - pos.point
                        if (diff.x == 0 || diff.y == 0 || abs(diff.x) == abs(diff.y)) {
                            gun.fire(entity, diff.dir)
                        } else null
                    } else null
                }
    }

    private fun goto(target: Position): Action? {
        return pos.level.findPath(pos.x, pos.y, target.x, target.y)?.firstOrNull()?.let { next ->
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

    private fun randomMove(): Action = Move(entity, (random.nextInt(3) - 1) to (random.nextInt(3) - 1))

    override val action: Action
        get() {
            if (lastResult?.succeeded == false) {
                return randomMove()
            }
            updateSenses()
            return dodge() ?: target?.let { shoot(it) ?: goto(it) ?: randomMove() } ?: randomMove()
        }
}