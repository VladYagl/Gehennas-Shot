package gehenna.level

import gehenna.utils.*

class StubLevelBuilder : BaseLevelBuilder<StubLevelBuilder.StubLevel>() {
    override fun build(): StubLevel {
        return StubLevel(width, height, 10 at 10).apply {
            box(0, 0, width, height)
            rect(0, 0, width, height)

            spawn(factory.new("rifle"), startPosition)
            spawn(factory.new("pistol"), startPosition)

//            for (pos in (11 to 11) until (width - 1 to height - 1)) {
//                spawn(factory.new("bandit"), pos)
//            }
        }
    }

    class StubLevel(width: Int, height: Int, override val startPosition: Point) : Level(width, height)
}
