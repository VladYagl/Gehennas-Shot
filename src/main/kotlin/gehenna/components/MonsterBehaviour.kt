package gehenna.components

import com.beust.klaxon.internal.firstNotNullResult
import gehenna.Action
import gehenna.Entity
import gehenna.Move
import gehenna.Shoot
import gehenna.utils.*
import java.lang.Math.abs

data class MonsterBehaviour(override val entity: Entity) : Behaviour() {
    private var target: Position? = null
    private val pos get() = entity[Position::class]!!

    private fun findTarget() {
        pos.visitFov { entity, _, _ ->
            if (entity.name == "player") {
                target = entity[Position::class]
            }
        }
    }

    private fun shoot(target: Position): Action? {
        return entity[Inventory::class]?.all()?.firstNotNullResult { it.entity[Gun::class] }?.let { gun ->
            val diff = target.point - pos.point
            if (diff.x == 0 || diff.y == 0 || abs(diff.x) == abs(diff.y)) {
                Shoot(entity, diff.dir, gun)
            } else null
        }
    }

    private fun goto(target: Position): Action? {
        return pos.level.findPath(pos.x, pos.y, target.x, target.y)?.firstOrNull()?.let {
            Move(entity, it.x - pos.x to it.y - pos.y)
        }
    }

    private fun dodge(): Action? {
        val zone = HashSet<Point>()
        val bullets = ArrayList<PredictableBehaviour>()
        pos.visitFov { entity, _, _ -> entity[BulletBehaviour::class]?.let { bullets.add(it) } }
        bullets.forEach { bullet ->
            zone.addAll(pos.level.predict(bullet, entity[Stats::class]?.speed?.toLong() ?: 100))
        }
        return if (pos.point in zone) {
            target?.point?.minus(pos.point)?.dir?.let { dir ->
                if (dir.plus(pos.point) !in zone) Move(entity, dir) else null
            } ?: directions.firstOrNull { it + pos.point !in zone }?.let { Move(entity, it) }
        } else null
    }

    private fun randomMove(): Action = Move(entity, (random.nextInt(3) - 1) to (random.nextInt(3) - 1))

    override val action: Action
        get() {
//            if (lastResult?.succeeded == false) {
//                return Move(entity, 0 to 0, 1000)
//            }
            findTarget()
            return dodge() ?: target?.let { shoot(it) ?: goto(it) ?: randomMove() } ?: randomMove()
        }
}