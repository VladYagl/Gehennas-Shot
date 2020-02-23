package asciiPanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.LookupOp;
import java.awt.image.ShortLookupTable;
import java.io.IOException;
import java.util.Objects;

/**
 * This simulates a code page 437 ASCII terminal display.
 *
 * @author Trystan Spangler
 */
public class AsciiPanel extends JPanel {
    private static final long serialVersionUID = -4167851861147593092L;

    public static final Color black = new Color(0, 0, 0);

    public static final Color red = new Color(128, 0, 0);

    public static final Color green = new Color(0, 128, 0);

    public static final Color yellow = new Color(128, 128, 0);

    public static final Color blue = new Color(0, 0, 128);

    public static final Color magenta = new Color(128, 0, 128);

    public static final Color cyan = new Color(0, 128, 128);

    public static final Color white = new Color(192, 192, 192);

    public static final Color brightBlack = new Color(128, 128, 128);

    public static final Color brightRed = new Color(255, 0, 0);

    public static final Color brightGreen = new Color(0, 255, 0);

    public static final Color brightYellow = new Color(255, 255, 0);

    public static final Color brightBlue = new Color(0, 0, 255);

    public static final Color brightMagenta = new Color(255, 0, 255);

    public static final Color brightCyan = new Color(0, 255, 255);

    public static final Color brightWhite = new Color(255, 255, 255);

    private Image offscreenBuffer;
    private Graphics offscreenGraphics;
    private int widthInCharacters;
    private int heightInCharacters;
    private int charWidth = 9;
    private int charHeight = 16;
    private String terminalFontFile = "cp437_9x16.png";
    private Color defaultBackgroundColor;
    private Color defaultForegroundColor;
    private int cursorX;
    private int cursorY;
    private BufferedImage glyphSprite;
    private BufferedImage[] glyphs;
    private char[][] chars;
    private Color[][] backgroundColors;
    private Color[][] foregroundColors;
    private char[][] oldChars;
    private Color[][] oldBackgroundColors;
    private Color[][] oldForegroundColors;
    private AsciiFont asciiFont;

    private void checkChar(char character) {
        if (character >= glyphs.length)
            throw new IllegalArgumentException("character " + character + " must be within range [0," + glyphs.length + "].");
    }

    private void checkX(int x) {
        if (x < 0 || x >= widthInCharacters)
            throw new IllegalArgumentException("x " + x + " must be within range [0," + widthInCharacters + ").");
    }

    private void checkY(int y) {
        if (y < 0 || y >= heightInCharacters)
            throw new IllegalArgumentException("y " + y + " must be within range [0," + heightInCharacters + ").");
    }

    private void checkInBounds(int x, int y) {
        checkX(x);
        checkY(y);
    }

    private void checkBoxBounds(int x, int y, int width, int height) {
        if (width < 1)
            throw new IllegalArgumentException("width " + width + " must be greater than 0.");

        if (height < 1)
            throw new IllegalArgumentException("height " + height + " must be greater than 0.");

        if (x + width > widthInCharacters)
            throw new IllegalArgumentException("x + width " + (x + width) + " must be less than " + (widthInCharacters + 1) + ".");

        if (y + height > heightInCharacters)
            throw new IllegalArgumentException("y + height " + (y + height) + " must be less than " + (heightInCharacters + 1) + ".");
    }

    public int getCharHeight() {
        return charHeight;
    }

    public int getCharWidth() {
        return charWidth;
    }

    public int getHeightInCharacters() {
        return heightInCharacters;
    }

    public int getWidthInCharacters() {
        return widthInCharacters;
    }

    public int getCursorX() {
        return cursorX;
    }

    public void setCursorX(int cursorX) {
        checkX(cursorX);

        this.cursorX = cursorX;
    }

    public int getCursorY() {
        return cursorY;
    }

    public void setCursorY(int cursorY) {
        checkY(cursorY);

        this.cursorY = cursorY;
    }

    public void setCursorPosition(int x, int y) {
        setCursorX(x);
        setCursorY(y);
    }

    public Color getDefaultBackgroundColor() {
        return defaultBackgroundColor;
    }

    public void setDefaultBackgroundColor(Color defaultBackgroundColor) {
        if (defaultBackgroundColor == null)
            throw new NullPointerException("defaultBackgroundColor must not be null.");

        this.defaultBackgroundColor = defaultBackgroundColor;
    }

    public Color getDefaultForegroundColor() {
        return defaultForegroundColor;
    }

    public void setDefaultForegroundColor(Color defaultForegroundColor) {
        if (defaultForegroundColor == null)
            throw new NullPointerException("defaultForegroundColor must not be null.");

        this.defaultForegroundColor = defaultForegroundColor;
    }

    public AsciiFont getAsciiFont() {
        return asciiFont;
    }

    public void setAsciiFont(AsciiFont font) {
        if (this.asciiFont == font) {
            return;
        }
        this.asciiFont = font;

        this.charHeight = font.getHeight();
        this.charWidth = font.getWidth();
        this.terminalFontFile = font.getFontFilename();

        Dimension panelSize = new Dimension(charWidth * widthInCharacters, charHeight * heightInCharacters);
        setPreferredSize(panelSize);

        glyphs = new BufferedImage[256];

        offscreenBuffer = new BufferedImage(panelSize.width, panelSize.height, BufferedImage.TYPE_INT_RGB);
        offscreenGraphics = offscreenBuffer.getGraphics();

        loadGlyphs();

        oldChars = new char[widthInCharacters][heightInCharacters];
    }

    public AsciiPanel() {
        this(80, 24);
    }

    public AsciiPanel(int width, int height) {
        this(width, height, null);
    }

    public AsciiPanel(int width, int height, AsciiFont font) {
        super();

        if (width < 1) {
            throw new IllegalArgumentException("width " + width + " must be greater than 0.");
        }

        if (height < 1) {
            throw new IllegalArgumentException("height " + height + " must be greater than 0.");
        }

        widthInCharacters = width;
        heightInCharacters = height;

        defaultBackgroundColor = black;
        defaultForegroundColor = white;

        chars = new char[widthInCharacters][heightInCharacters];
        backgroundColors = new Color[widthInCharacters][heightInCharacters];
        foregroundColors = new Color[widthInCharacters][heightInCharacters];

        oldBackgroundColors = new Color[widthInCharacters][heightInCharacters];
        oldForegroundColors = new Color[widthInCharacters][heightInCharacters];

        if (font == null) {
            font = AsciiFont.CP437_9x16;
        }
        setAsciiFont(font);
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    @Override
    public void paint(Graphics g) {
        if (g == null)
            throw new NullPointerException();

        for (int x = 0; x < widthInCharacters; x++) {
            for (int y = 0; y < heightInCharacters; y++) {
                if (oldBackgroundColors[x][y] == backgroundColors[x][y]
                        && oldForegroundColors[x][y] == foregroundColors[x][y]
                        && oldChars[x][y] == chars[x][y])
                    continue;

                Color bg = backgroundColors[x][y];
                Color fg = foregroundColors[x][y];

                LookupOp op = setColors(bg, fg);
                BufferedImage img = op.filter(glyphs[chars[x][y]], null);
                offscreenGraphics.drawImage(img, x * charWidth, y * charHeight, null);

                oldBackgroundColors[x][y] = backgroundColors[x][y];
                oldForegroundColors[x][y] = foregroundColors[x][y];
                oldChars[x][y] = chars[x][y];
            }
        }

        g.drawImage(offscreenBuffer, 0, 0, this);
    }

    private void loadGlyphs() {
        try {
            glyphSprite = ImageIO.read(Objects.requireNonNull(AsciiPanel.class.getClassLoader().getResource(terminalFontFile)));
        } catch (IOException e) {
            System.err.println("loadGlyphs(): " + e.getMessage());
        }

        for (int i = 0; i < 256; i++) {
            int sx = (i % 16) * charWidth;
            int sy = (i / 16) * charHeight;

            glyphs[i] = new BufferedImage(charWidth, charHeight, BufferedImage.TYPE_INT_ARGB);
            glyphs[i].getGraphics().drawImage(glyphSprite, 0, 0, charWidth, charHeight, sx, sy, sx + charWidth, sy + charHeight, null);
        }
    }

    /**
     * Create a <code>LookupOp</code> object (lookup table) mapping the original
     * pixels to the background and foreground colors, respectively.
     *
     * @param bgColor the background color
     * @param fgColor the foreground color
     * @return the <code>LookupOp</code> object (lookup table)
     */
    private LookupOp setColors(Color bgColor, Color fgColor) {
        short[] a = new short[256];
        short[] r = new short[256];
        short[] g = new short[256];
        short[] b = new short[256];

        byte bga = (byte) (bgColor.getAlpha());
        byte bgr = (byte) (bgColor.getRed());
        byte bgg = (byte) (bgColor.getGreen());
        byte bgb = (byte) (bgColor.getBlue());

        byte fga = (byte) (fgColor.getAlpha());
        byte fgr = (byte) (fgColor.getRed());
        byte fgg = (byte) (fgColor.getGreen());
        byte fgb = (byte) (fgColor.getBlue());

        for (int i = 0; i < 256; i++) {
            if (i == 0) {
                a[i] = bga;
                r[i] = bgr;
                g[i] = bgg;
                b[i] = bgb;
            } else {
                a[i] = fga;
                r[i] = fgr;
                g[i] = fgg;
                b[i] = fgb;
            }
        }

        short[][] table = {r, g, b, a};
        return new LookupOp(new ShortLookupTable(0, table), null);
    }

    public AsciiPanel clear() {
        return clear(' ', 0, 0, widthInCharacters, heightInCharacters, defaultForegroundColor, defaultBackgroundColor);
    }

    public AsciiPanel clear(char character) {
        return clear(character, 0, 0, widthInCharacters, heightInCharacters, defaultForegroundColor, defaultBackgroundColor);
    }

    public AsciiPanel clear(char character, Color foreground, Color background) {
        return clear(character, 0, 0, widthInCharacters, heightInCharacters, foreground, background);
    }

    public AsciiPanel clear(char character, int x, int y, int width, int height) {
        return clear(character, x, y, width, height, defaultForegroundColor, defaultBackgroundColor);
    }

    public AsciiPanel clear(char character, int x, int y, int width, int height, Color foreground, Color background) {
        checkChar(character);
        checkInBounds(x, y);
        checkBoxBounds(x, y, width, height);

        int originalCursorX = cursorX;
        int originalCursorY = cursorY;
        for (int xo = x; xo < x + width; xo++) {
            for (int yo = y; yo < y + height; yo++) {
                write(character, xo, yo, foreground, background);
            }
        }
        cursorX = originalCursorX;
        cursorY = originalCursorY;

        return this;
    }

    public AsciiPanel write(char character) {
        return write(character, cursorX, cursorY, defaultForegroundColor, defaultBackgroundColor);
    }

    public AsciiPanel write(char character, Color foreground) {
        return write(character, cursorX, cursorY, foreground, defaultBackgroundColor);
    }

    public AsciiPanel write(char character, Color foreground, Color background) {
        return write(character, cursorX, cursorY, foreground, background);
    }

    public AsciiPanel write(char character, int x, int y) {
        return write(character, x, y, defaultForegroundColor, defaultBackgroundColor);
    }

    public AsciiPanel write(char character, int x, int y, Color foreground) {
        return write(character, x, y, foreground, defaultBackgroundColor);
    }

    public AsciiPanel write(char character, int x, int y, Color foreground, Color background) {
        checkChar(character);
        checkInBounds(x, y);

        if (foreground == null) foreground = defaultForegroundColor;
        if (background == null) background = defaultBackgroundColor;

        chars[x][y] = character;
        foregroundColors[x][y] = foreground;
        backgroundColors[x][y] = background;
        cursorX = x + 1;
        cursorY = y;
        return this;
    }

    public AsciiPanel write(String string) {
        return write(string, cursorX, cursorY, defaultForegroundColor, defaultBackgroundColor);
    }

    public AsciiPanel write(String string, Color foreground) {
        return write(string, cursorX, cursorY, foreground, defaultBackgroundColor);
    }

    public AsciiPanel write(String string, Color foreground, Color background) {
        return write(string, cursorX, cursorY, foreground, background);
    }

    public AsciiPanel write(String string, int x, int y) {
        return write(string, x, y, defaultForegroundColor, defaultBackgroundColor);
    }

    public AsciiPanel write(String string, int x, int y, Color foreground) {
        return write(string, x, y, foreground, defaultBackgroundColor);
    }

    public AsciiPanel write(String string, int x, int y, Color foreground, Color background) {
        if (string == null)
            throw new NullPointerException("string must not be null.");

        checkX(x + string.length() - 1);
        checkInBounds(x, y);

        for (int i = 0; i < string.length(); i++) {
            write(string.charAt(i), x + i, y, foreground, background);
        }
        return this;
    }

    public AsciiPanel changeCharColors(int x, int y, Color foreground, Color background) {
        checkInBounds(x, y);

        foregroundColors[x][y] = foreground;
        backgroundColors[x][y] = background;
        return this;
    }

    public AsciiPanel writeCenter(String string, int y) {
        return writeCenter(string, y, defaultForegroundColor, defaultBackgroundColor);
    }

    public AsciiPanel writeCenter(String string, int y, Color foreground) {
        return writeCenter(string, y, foreground, defaultBackgroundColor);
    }

    public AsciiPanel writeCenter(String string, int y, Color foreground, Color background) {
        if (string == null)
            throw new NullPointerException("string must not be null.");

        if (string.length() > widthInCharacters)
            throw new IllegalArgumentException("string.length() " + string.length() + " must be less than " + widthInCharacters + ".");

        int x = (widthInCharacters - string.length()) / 2;

        return write(string, x, y, foreground, background);
    }

    public void withEachTile(TileTransformer transformer) {
        withEachTile(0, 0, widthInCharacters, heightInCharacters, transformer);
    }

    public void withEachTile(int left, int top, int width, int height, TileTransformer transformer) {
        AsciiCharacterData data = new AsciiCharacterData();

        for (int x0 = 0; x0 < width; x0++)
            for (int y0 = 0; y0 < height; y0++) {
                int x = left + x0;
                int y = top + y0;

                if (x < 0 || y < 0 || x >= widthInCharacters || y >= heightInCharacters)
                    continue;

                data.character = chars[x][y];
                data.foregroundColor = foregroundColors[x][y];
                data.backgroundColor = backgroundColors[x][y];

                transformer.transformTile(x, y, data);

                chars[x][y] = data.character;
                foregroundColors[x][y] = data.foregroundColor;
                backgroundColors[x][y] = data.backgroundColor;
            }
    }
}
