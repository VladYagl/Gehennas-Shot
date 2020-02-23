package gehenna.ui.panel

import gehenna.utils.Point
import gehenna.utils.Size
import gehenna.utils.at
import gehenna.utils.*
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.image.LookupOp
import java.awt.image.ShortLookupTable
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JPanel

/**
 * This simulates a code page 437 ASCII terminal display.
 *
 * @author of java version Trystan Spangler
 */
open class AsciiPanel(val size: Size, font: AsciiFont, var defaultFg: Color = white, var defaultBg: Color = black) : JPanel() {

    private val charWidth = font.width
    private val charHeight = font.height
    private var terminalFontFile: String = font.fontFilename

    private val chars = CharArray(size)
    private var oldChars = CharArray(size)

    private val bgColors = Array(size) { defaultBg }
    private val fgColors = Array(size) { defaultFg }
    private val oldFgColors = Array<Color?>(size) { null }
    private val oldBgColors = Array<Color?>(size) { null }

    private val panelSize = Dimension(charWidth * size.width, charHeight * size.height)
    private var offscreenBuffer: Image = BufferedImage(panelSize.width, panelSize.height, BufferedImage.TYPE_INT_RGB)
    private var offscreenGraphics: Graphics = (offscreenBuffer as BufferedImage).graphics
    private var glyphSprite: BufferedImage? = null
    private var glyphs: Array<BufferedImage?> = arrayOfNulls(256)


    protected var cursor: Point = 0 at 0
//        set(point) {
//            checkInBounds(point.x, point.y)
//            field = point
//        }

    private fun moveCursor() {
        cursor = if (cursor.x == size.width - 1) {
            0 at cursor.y + 1
        } else {
            (cursor.x + 1) at cursor.y
        }
    }

    private fun checkChar(character: Char) {
        assert(character.toInt() < glyphs.size) { "character " + character + " must be within range [0," + glyphs.size + "]." }
    }

    private fun checkInBounds(x: Int, y: Int) {
        assert(size.contains(x at y)) { "x:$x must be in range [0, ${size.width}] and y:$y must be in range [0, ${size.height}]" }
    }

    private fun checkBoxBounds(x: Int, y: Int, width: Int, height: Int) {
        assert(width >= 1) { "width $width must be greater than 0." }
        assert(height >= 1) { "height $height must be greater than 0." }
        assert(x + width <= size.width) { "x + width " + (x + width) + " must be less than " + (size.width + 1) + "." }
        assert(y + height <= size.height) { "y + height " + (y + height) + " must be less than " + (size.height + 1) + "." }
    }

    override fun update(g: Graphics) {
        paint(g)
    }

    override fun paint(g: Graphics?) {
        if (g == null) throw NullPointerException()
        for (x in 0 until size.width) {
            for (y in 0 until size.height) {
                if (oldBgColors[x][y] === bgColors[x][y] && oldFgColors[x][y] === fgColors[x][y] && oldChars[x][y] == chars[x][y]) continue
                val bg = bgColors[x][y]
                val fg = fgColors[x][y]
                val op = setColors(bg, fg)
                val img = op.filter(glyphs[chars[x][y].toInt()], null)
                offscreenGraphics.drawImage(img, x * charWidth, y * charHeight, null)
                oldBgColors[x][y] = bgColors[x][y]
                oldFgColors[x][y] = fgColors[x][y]
                oldChars[x][y] = chars[x][y]
            }
        }
        g.drawImage(offscreenBuffer, 0, 0, this)
    }

    private fun loadGlyphs() {
        try {
            glyphSprite = ImageIO.read(Objects.requireNonNull(AsciiPanel::class.java.classLoader.getResource(terminalFontFile)))
        } catch (e: IOException) {
            System.err.println("loadGlyphs(): " + e.message)
        }
        for (i in 0..255) {
            val sx = i % 16 * charWidth
            val sy = i / 16 * charHeight
            glyphs[i] = BufferedImage(charWidth, charHeight, BufferedImage.TYPE_INT_ARGB)
            glyphs[i]!!.graphics.drawImage(glyphSprite, 0, 0, charWidth, charHeight, sx, sy, sx + charWidth, sy + charHeight, null)
        }
    }

    /**
     * Create a `LookupOp` object (lookup table) mapping the original
     * pixels to the background and foreground colors, respectively.
     *
     * @return the `LookupOp` object (lookup table)
     */
    private fun setColors(bgColor: Color?, fgColor: Color?): LookupOp {
        val a = ShortArray(256)
        val r = ShortArray(256)
        val g = ShortArray(256)
        val b = ShortArray(256)
        val bga = bgColor!!.alpha.toByte()
        val bgr = bgColor.red.toByte()
        val bgg = bgColor.green.toByte()
        val bgb = bgColor.blue.toByte()
        val fga = fgColor!!.alpha.toByte()
        val fgr = fgColor.red.toByte()
        val fgg = fgColor.green.toByte()
        val fgb = fgColor.blue.toByte()
        for (i in 0..255) {
            if (i == 0) {
                a[i] = bga.toShort()
                r[i] = bgr.toShort()
                g[i] = bgg.toShort()
                b[i] = bgb.toShort()
            } else {
                a[i] = fga.toShort()
                r[i] = fgr.toShort()
                g[i] = fgg.toShort()
                b[i] = fgb.toShort()
            }
        }
        val table = arrayOf(r, g, b, a)
        return LookupOp(ShortLookupTable(0, table), null)
    }

    /**
     * Clear the section of the screen with the specified character and whatever
     * the specified foreground and background colors are.
     */
    @JvmOverloads
    fun clear(
            character: Char = ' ',
            x: Int = 0,
            y: Int = 0,
            width: Int = size.width,
            height: Int = size.height,
            fg: Color = defaultFg,
            bg: Color = defaultBg
    ): AsciiPanel {
        checkChar(character)
        checkInBounds(x, y)
        checkBoxBounds(x, y, width, height)
        val originalCursor = cursor
        for (xo in x until x + width) {
            for (yo in y until y + height) {
                write(character, xo, yo, fg, bg, false)
            }
        }
        cursor = originalCursor
        return this
    }

    /**
     * Write a character to the specified position with the specified foreground and background colors.
     * This updates the cursor's position but not the default foreground or background colors.
     */
    @JvmOverloads
    fun write(
            character: Char,
            x: Int = cursor.x,
            y: Int = cursor.y,
            fg: Color = defaultFg,
            bg: Color = defaultBg,
            moveCursor: Boolean = true
    ): AsciiPanel {
        checkChar(character)
        checkInBounds(x, y)
        chars[x][y] = character
        fgColors[x][y] = fg
        bgColors[x][y] = bg
        if (moveCursor) moveCursor()
        return this
    }

    /**
     * Write a string to the specified position with the specified foreground and background colors.
     * Automatically wraps string at the end of the line.
     *
     * This updates the cursor's position but not the default foreground or background colors.
     */
    @JvmOverloads
    fun write(
            string: String,
            x: Int = cursor.x,
            y: Int = cursor.y,
            fg: Color = defaultFg,
            bg: Color = defaultBg
    ): AsciiPanel {
        for (i in string.indices) {
            write(string[i], x + i, y, fg, bg)
        }
        return this
    }

    /**
     * Updates foreground and background colors of char
     */
    fun changeCharColors(x: Int, y: Int, fg: Color, bg: Color): AsciiPanel {
        checkInBounds(x, y)
        fgColors[x][y] = fg
        bgColors[x][y] = bg
        return this
    }

    /**
     * Applies transformer to each tile in (left, top)-(left+width, top+height) rectangle
     * Transformer may change TileData, this changes will be applied to actual characters on panel
     */
    fun withEachTile(
            left: Int = 0,
            top: Int = 0,
            width: Int = size.width,
            height: Int = size.height,
            transformer: (Int, Int, TileData) -> Unit
    ) {
        for (x0 in 0 until width) for (y0 in 0 until height) {
            val x = left + x0
            val y = top + y0
            if (x < 0 || y < 0 || x >= size.width || y >= size.height) continue
            val data = TileData(chars[x][y], fgColors[x][y], bgColors[x][y])
            transformer.invoke(x, y, data)
            chars[x][y] = data.character
            fgColors[x][y] = data.fgColor
            bgColors[x][y] = data.bgColor
        }
    }

    init {
        require(size.width >= 1) { "width ${size.width} must be greater than 0." }
        require(size.height >= 1) { "height ${size.height} must be greater than 0." }

        preferredSize = panelSize
        loadGlyphs()
    }

    data class TileData(var character: Char, var fgColor: Color, var bgColor: Color)
    data class AsciiFont(val fontFilename: String, val width: Int, val height: Int)
}