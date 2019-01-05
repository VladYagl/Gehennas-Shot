package gehenna.ui

import gehenna.Game
import gehenna.JsonFactory
import gehenna.Settings
import gehenna.components.*
import gehenna.components.behaviour.PredictableBehaviour
import gehenna.level.DungeonLevel
import gehenna.streamResource
import gehenna.utils.*
import java.awt.Color

class App(private val ui: UI, private val settings: Settings) {
    private val factory = JsonFactory()
    private val game = Game(factory)
    private val context: Context
    private var state: State

    private var time = 0L
    private var needRepaint = true
    private var stop = false
    fun mainLoop() {
        try {
            var count = 0
            var repaintCount = 0
            while (true) {
                if (settings.drawEachUpdate) needRepaint = needRepaint || game.time > time + settings.updateStep
                if (game.isPlayerNext()) needRepaint = true
                if (needRepaint) { // fixme - when not updating by game time it has weird stops???
                    time = game.time
                    ui.info.writeLine("Paint=${repaintCount++} loop=$count", 0)
                    update()
                    ui.update()
                }

                game.update()
                count++

                if (!game.player.has(Position::class)) {
                    state = End(context)
                    ui.endGame()
                    return
                }
            }
        } catch (e: Throwable) {
            showError(e)
            ui.printException(e)
        }
    }

    private fun update() {
        updateLog()
        drawWorld()
        predict()
        updateInfo()

        needRepaint = false
    }

    private fun updateLog() {
        game.player[Logger::class]?.log?.let { messages ->
            ui.updateLog(messages)
        }
    }

    private fun updateInfo() {
        ui.info.writeLine("In game time: " + game.time, 1)
        val glyph = game.player[Glyph::class]!!
        val pos = game.player[Position::class]!!
        val storage = game.player[Inventory::class]!!
        ui.info.writeLine("Player glyph = ${glyph.char}|${glyph.priority}", 2)
        ui.info.writeLine("Player pos = ${pos.x}, ${pos.y}", 3)
        ui.info.writeLine("Player hp = " + game.player[Health::class]?.current, 4)
        ui.info.writeLine("Effects = " + game.player.all(Effect::class), 5)
        ui.info.writeLine("Inventory", 8, Alignment.center, Color.white, Color.darkGray)
        storage.all().forEachIndexed { index, item ->
            ui.info.writeLine(item.entity.toString(), 9 + index)
        }

        if (pos.level is DungeonLevel) {
            ui.info.writeLine("Level: " + pos.level.depth, 19)
        }

        ui.info.writeLine("Objects", 30, Alignment.center, Color.white, Color.darkGray)
        pos.neighbors.forEachIndexed { index, entity ->
            ui.info.writeLine(entity.toString(), 31 + index)
        }
    }

    private var camera = 0 to 0
    private val cameraBound = ui.worldWidth / 2 - 5 to ui.worldHeight / 2 - 5
    private fun moveCamera(playerPos: Point) {
        var x = camera.x
        var y = camera.y
        if (playerPos.x < camera.x + cameraBound.x) {
            x = playerPos.x - cameraBound.x
        }
        if (playerPos.y < camera.y + cameraBound.y) {
            y = playerPos.y - cameraBound.y
        }
        val end = camera.x + ui.worldWidth to camera.y + ui.worldHeight
        if (playerPos.x > end.x - cameraBound.x) {
            x = playerPos.x + cameraBound.x - ui.worldWidth
        }
        if (playerPos.y > end.y - cameraBound.y) {
            y = playerPos.y + cameraBound.y - ui.worldHeight
        }

        camera = x to y
    }

    private fun inView(x: Int, y: Int): Boolean {
        return camera.x <= x && camera.y <= y && camera.x + ui.worldWidth > x && camera.y + ui.worldHeight > y
    }

    private fun viewPoint(x: Int, y: Int): Point {
        return (x to y) - camera
    }

    private fun levelPos(x: Int, y: Int): Point {
        return (x to y) + camera
    }

    private val priority = Array(11 * 8) { Array(8 * 8) { -2 } }
    private fun putGlyph(glyph: Glyph, x: Int, y: Int, color: Color? = null) {
        if (inView(x, y)) {
            val p = viewPoint(x, y)
            if (glyph.priority > priority[p.x, p.y]) {
                ui.world.putChar(glyph.char, p.x, p.y, color)
                priority[p.x, p.y] = glyph.priority
            }
        }
    }

    private fun drawWorld() {
        //TODO: Try drawing whole level and then moving it
        //TODO: Animations
        priority.forEach { it.fill(-100) }
        val playerPos = game.player[Position::class]!!
        val level = playerPos.level
        moveCamera(playerPos.point)
        game.player.all(Senses::class).forEach { sense ->
            sense.visitFov { entity, x, y ->
                entity[Glyph::class]?.let { glyph ->
                    if (glyph.memorable) level.remember(x, y, glyph)
                    putGlyph(glyph, x, y)
                }
            }
        }

        for ((x, y) in range(ui.worldWidth, ui.worldHeight)) {
            val pos = levelPos(x, y)
            if (priority[x, y] == -100 && pos.x < level.width && pos.y < level.height && pos.x >= 0 && pos.y >= 0) {
                level.memory(pos.x, pos.y)?.let {
                    putGlyph(it, pos.x, pos.y, Color(96, 32, 32))
                } ?: putGlyph(Glyph(game.player, ' ', -2), pos.x, pos.y)
            } else {
                putGlyph(Glyph(game.player, ' ', -2), pos.x, pos.y)
            }
        }
    }

    private fun predict() {
        val playerPos = game.player[Position::class]!!
        val stats = game.player[Stats::class]!!
        val level = playerPos.level
        val behaviours = ArrayList<PredictableBehaviour>()
        val sight = game.player[Senses.Sight::class]!!
        sight.visitFov { entity, _, _ ->
            entity.all(PredictableBehaviour::class).firstOrNull()?.let { behaviours.add(it) }
        }
        behaviours.forEach {
            val glyph = it.entity[Glyph::class]!!
            var color = Color.white * 0.5 // TODO : DEFAULT COLOR
            val prediction = level.predict(it, stats.speed.toLong())
            prediction.forEach { (x, y) ->
                if (sight.isVisible(x, y) && inView(x, y)) {
                    color *= 0.85
                    putGlyph(glyph, x, y, color)
                }
            }

        }
    }

    init {
        factory.loadJson(streamResource("data/entities.json"))
        factory.loadJson(streamResource("data/items.json"))
        game.init()

        game.player[Logger::class]?.add("Welcome to Gehenna's Shot")
        game.player[Logger::class]?.add("   Suffer bitch,   love you " + 3.toChar())
        context = Context(game, ui)
        state = State.create(context)
    }

    fun onInput(input: Input) {
        state = state.handleInput(input)
        needRepaint = true
    }
}