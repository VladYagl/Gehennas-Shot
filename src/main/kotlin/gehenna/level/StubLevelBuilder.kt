package gehenna.level

import gehenna.utils.*
import gehenna.utils.Point.Companion.zero

class StubLevelBuilder : BaseLevelBuilder<StubLevelBuilder.StubLevel>() {
    override fun build(): StubLevel {
        return StubLevel(size, 10 at 10).apply {
            box(zero, size)
            rect(zero, size)

            spawn(factory.new("rifle"), startPosition)
            spawn(factory.new("pistol"), startPosition)

            spawn(factory.new("bandit"), 15 at 10)

//            for (pos in (11 to 11) until (width - 1 to height - 1)) {
//                spawn(factory.new("bandit"), pos)
//            }
        }
    }

    class StubLevel(size: Size, override val startPosition: Point) : Level(size)
}
