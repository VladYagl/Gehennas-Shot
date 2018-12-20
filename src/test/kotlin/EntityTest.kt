import org.junit.jupiter.api.Test
import kotlin.test.*

class EntityTest : ManagerTest() {
    @Test
    fun addingRemovingComponent() {
        val player = Entity("Player")
        player.add(Glyph(player, '@'))
        player.add(Obstacle(player, true, true))

        val monster = Entity("Kobold")
        monster.add(Glyph(monster, 'k'))
        monster.add(Obstacle(monster, true, false))

        val position = player[Position::class]
        assertNull(position)

        val glyph = player[Glyph::class]
        assertEquals('@', glyph?.char)
        val monsterGlyph = monster[Glyph::class]
        assertEquals('k', monsterGlyph?.char)

        val solid = player[Obstacle::class]
        assertNotNull(solid)
        assertTrue(solid.blockMove)
        assertTrue(solid.blockView)

        assert(ComponentManager[Position::class].isEmpty())
        val glyphs = ComponentManager[Glyph::class]
        assertEquals(2, glyphs.size)
        assert(glyphs.contains(player))
        assert(glyphs.contains(monster))

        assertEquals("Player", glyphs[glyphs.indexOf(player)].name)
        assertEquals("Kobold", glyphs[glyphs.indexOf(monster)].name)
    }
}
