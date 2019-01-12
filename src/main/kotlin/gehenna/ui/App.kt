package gehenna.ui

import gehenna.component.*
import gehenna.component.behaviour.PredictableBehaviour
import gehenna.component.behaviour.ThinkUntilSet
import gehenna.core.Game
import gehenna.factory.EntityFactory
import gehenna.factory.LevelPartFactory
import gehenna.level.DungeonLevelBuilder
import gehenna.utils.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import java.awt.Color

class App(private val ui: UI, private val settings: Settings) {
    private val factory = EntityFactory()
    private val levelFactory = LevelPartFactory(factory)
    private val game = Game(factory, levelFactory)
    private val context: UIContext
    private var state: State

    private var time = 0L
    private var count = 0
    private var repaintCount = 0
    private var stop = false
    suspend fun mainLoop() {
        try {
            GlobalScope.launch {
                uiLoop()
            }
            while (true) {
                if ((settings.drawEachUpdate && game.time > time + settings.updateStep) || game.isPlayerNext())
                    repaint.offer(Unit)

                withContext(repaintContext) {
                    game.update()
                }
                count++

                if (!game.player.has(Position::class)) {
                    repaint.offer(Unit)
                    state = End(context)
                    val window = ui.newWindow(17, 6)
                    window.writeLine("RIP ", 1, Alignment.center)
                    window.writeLine("YOU ARE DEAD", 3, Alignment.center)
                    return
                }
                if ((game.player[Position::class]?.level as DungeonLevelBuilder.DungeonLevel).depth == 2) {
                    repaint.offer(Unit)
                    state = End(context)
                    val window = ui.newWindow(19, 4)
                    window.writeLine("WE WON ZULUL", 1, Alignment.center)
                    return
                }
            }
        } catch (e: Throwable) {
            showError(e)
            ui.printException(e)
        }
    }

    //    private val repaint = Channel<Boolean>(Channel.CONFLATED)
    private val repaintContext = newSingleThreadContext("CounterContext")
    private val repaint = Channel<Unit>(Channel.CONFLATED)
    private suspend fun uiLoop() {
        while (true) {
//        for (i in repaint) {
            time = game.time
            ui.info.writeLine("Paint=${repaintCount++} loop=$count", 0)
            withContext(repaintContext) {
                update()
            }
            ui.update()
        }
    }

    private fun update() {
        updateLog()
        drawWorld()
        predict()
        updateInfo()
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
        ui.info.writeLine("Player position = ${pos.x}, ${pos.y}", 3)
        ui.info.writeLine("Player hp = " + game.player[Health::class]?.current, 4)
        ui.info.writeLine("Effects = " + game.player.all(Effect::class), 5)
        ui.info.writeLine("Inventory", 8, Alignment.center, Color.white, Color.darkGray)
        repeat(10) { i -> ui.info.writeLine("", 9 + i) }
        storage.all().forEachIndexed { index, item ->
            ui.info.writeLine(item.entity.toString(), 9 + index)
        }

        repeat(10) { i -> ui.info.writeLine("", 31 + i) }
        ui.info.writeLine("Objects", 30, Alignment.center, Color.white, Color.darkGray)
        pos.neighbors.forEachIndexed { index, entity ->
            ui.info.writeLine(entity.toString(), 31 + index)
        }
    }

    private var camera = 0 to 0
    private val cameraBound = ui.worldWidth / 2 - 30 to ui.worldHeight / 2 - 30
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

    private val priority = Array(ui.worldWidth) { Array(ui.worldHeight) { -2 } }
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

        range(ui.worldWidth, ui.worldHeight).forEach { (x, y) ->
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
            var color = Color.white * 0.5 // TODO : DEFAULT COLOR
            val prediction = level.predictWithGlyph(it, game.player[ThinkUntilSet::class]!!.time + stats.speed.toLong())
            prediction.forEach { (p, glyph) ->
                if (sight.isVisible(p.x, p.y) && inView(p.x, p.y)) {
                    color *= 0.85
                    putGlyph(glyph, p.x, p.y, color)
                }
            }

        }
    }

    init {
        factory.loadJson(streamResource("data/entities.json"))
        factory.loadJson(streamResource("data/items.json"))
        levelFactory.loadJson(streamResource("data/rooms.json"))
        game.init()

        game.player[Logger::class]?.add("Welcome to Gehenna's Shot")
        game.player[Logger::class]?.add("   Suffer bitch,   love you " + 3.toChar())
        context = UIContext(game, ui)
        state = State.create(context)
    }

    fun onInput(input: Input) {
        ui.info.writeLine("$input", 23)
        state = state.handleInput(input)
        repaint.offer(Unit)
    }
}