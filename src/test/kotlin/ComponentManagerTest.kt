import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ComponentManagerTest : ManagerTest() {

    companion object {

        private lateinit var floor: Entity
        private lateinit var wall: Entity
        private lateinit var item: Entity

        @BeforeAll
        @JvmStatic
        fun init() {
            floor = Entity("Floor")
            wall = Entity("Wall")
            item = Entity("Sword")

            Obstacle(floor, blockMove = false, blockView = false)
            Floor(floor)
            Glyph(floor, '.')

            Obstacle(wall, blockMove = true, blockView = true)
            Glyph(wall, '.')

            Glyph(item, '(')
        }
    }

    @Test
    fun remove() {
        floor.remove(floor[Glyph::class]!!)
        assertNull(floor[Glyph::class])
        assertEquals(2, ComponentManager[Glyph::class].size)
        assertEquals(2, ComponentManager.all(Glyph::class).size)
        Glyph(floor, '.')
    }

    @Test
    fun get() {
        assertEquals(3, ComponentManager[Glyph::class].size)
        assertEquals(2, ComponentManager[Glyph::class, Obstacle::class].size)
        assertEquals(1, ComponentManager[Glyph::class, Floor::class].size)
    }

    @Test
    fun all() {
        println(ComponentManager.all(Glyph::class))
        assertEquals(3, ComponentManager.all(Glyph::class).size)
    }
}