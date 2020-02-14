package gehenna.level

import gehenna.core.Context
import gehenna.utils.Point
import gehenna.utils.Point.Companion.zero
import gehenna.utils.Size
import gehenna.utils.at

class StubLevelFactory(context: Context) : BaseLevelFactory<Level>(context) {
    override fun build(previous: Level?, backPoint: Point?): Pair<Level, Point> {
        val startPosition = 10 at 10
        return Pair(Level(size).apply {
            box(zero, size)
            rect(zero, size)
            box(zero, Size(50, 50))

            spawn(factory.new("rifle"), startPosition)
            spawn(factory.new("pistol"), startPosition)

//            for (pos in (5 at 5) until (15 at 15)) {
//                if (random.nextDouble() > 0.9) spawn(factory.new("soloBandit"), pos)
//            }

//            for (pos in (11 to 11) until (width - 1 to height - 1)) {
//                spawn(factory.new("bandit"), pos)
//            }
        }, startPosition)
    }
}
