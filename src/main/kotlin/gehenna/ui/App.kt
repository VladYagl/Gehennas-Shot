package gehenna.ui

import gehenna.component.*
import gehenna.component.behaviour.PredictableBehaviour
import gehenna.component.behaviour.ThinkUntilSet
import gehenna.core.Entity
import gehenna.core.Game
import gehenna.factory.EntityFactory
import gehenna.factory.LevelPartFactory
import gehenna.level.DungeonLevelBuilder
import gehenna.utils.*
import kotlinx.coroutines.*
import java.awt.Color
import kotlin.reflect.full.safeCast
import kotlin.system.measureNanoTime

class App(private val ui: UI, private val settings: Settings) : InputListener {
    private val minPriority = Int.MIN_VALUE

    private val factory = EntityFactory()
    private val levelFactory = LevelPartFactory(factory)
    private val game = Game(factory, levelFactory)
    private val context = UIContext(game, ui)
    private var state = State.create(context)

    private var time = 0L
    private var count = 0
    fun start() {
        factory.loadJson(streamResource("data/entities.json"))
        factory.loadJson(streamResource("data/items.json"))
        levelFactory.loadJson(streamResource("data/rooms.json"))
        game.init()

        game.player<Logger>()?.add("Welcome to Gehenna's Shot")
        game.player<Logger>()?.add("Suffer bitch, love you " + 3.toChar())
        val uiJob = GlobalScope.launch(exceptionHandler) {
            uiLoop()
        }
        GlobalScope.launch(exceptionHandler) {
            while (gameLoop(uiJob)) {
            }
        }
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        showError(exception)
        ui.printException(exception)
    }

    private suspend fun gameLoop(uiJob: Job): Boolean {
        return withContext(gameContext) {
            game.update()

            when {
                !game.player.has<Position>() -> {
                    state = End(context)
                    val window = ui.newWindow(17, 6)
                    window.writeLine("RIP ", 1, Alignment.center)
                    window.writeLine("YOU ARE DEAD", 3, Alignment.center)
                    uiJob.cancel()
                    uiJob.join()
                    false
                }
                DungeonLevelBuilder.DungeonLevel::class.safeCast(game.player<Position>()?.level)?.depth == 2 -> { //fixme
                    state = End(context)
                    val window = ui.newWindow(19, 4)
                    window.writeLine("WE WON ZULUL", 1, Alignment.center)
                    uiJob.cancel()
                    uiJob.join()
                    false
                }
                else -> true
            }
        }
    }

    private val gameContext = newSingleThreadContext("GameContext")
    private suspend fun uiLoop() {
        while (true) {
//        for (i in repaint) {
            val fps = 1000_000_000L / measureNanoTime {
                time = game.time
                withContext(gameContext) {
                    update()
                }
                ui.update()
            }
            ui.info.writeLine("fps=$fps loop=$count", 0)
        }
    }

    private fun update() {
        updateLog()
        drawWorld()
        updateInfo()
    }

    private fun updateLog() {
        game.player<Logger>()?.log?.let { messages ->
            ui.updateLog(messages)
        }
    }

    private fun updateInfo() {
        ui.info.writeLine("In game waitTime: " + game.time, 1)
        val glyph = game.player<Glyph>()!!
        val pos = game.player<Position>()!!
        val storage = game.player<Inventory>()!!
        ui.info.writeLine("Player glyph = ${glyph.char}|${glyph.priority}", 2)
        ui.info.writeLine("Player position = ${pos.x}, ${pos.y}", 3)
        ui.info.writeLine("Player hp = " + game.player<Health>()?.current, 4)
        ui.info.writeLine("Effects = " + game.player.all<Effect>(), 5)
        ui.info.writeLine("Inventory", 8, Alignment.center, ui.info.fgColor, Color.darkGray)
        repeat(10) { i -> ui.info.clearLine(9 + i) }
        storage.all().forEachIndexed { index, item ->
            ui.info.writeLine(item.entity.toString(), 9 + index)
        }

        repeat(10) { i -> ui.info.clearLine(31 + i) }
        ui.info.writeLine("Objects", 30, Alignment.center, ui.info.fgColor, Color.darkGray)
        pos.neighbors.forEachIndexed { index, entity ->
            ui.info.writeLine(entity.toString(), 31 + index)
        }
    }

    private var camera = 0 to 0
    private val cameraBound = ui.worldWidth / 2 - ui.worldWidth / 5 to ui.worldHeight / 2 - 3
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

    private val priority = Array(ui.worldWidth) { Array(ui.worldHeight) { minPriority } }
    private fun putGlyph(glyph: Glyph, x: Int, y: Int, fg: Color = ui.world.fgColor, bg: Color = ui.world.bgColor) {
        if (inView(x, y)) {
            val p = viewPoint(x, y)
            if (glyph.priority > priority[p.x, p.y]) {
                ui.world.putChar(glyph.char, p.x, p.y, fg, bg)
                priority[p.x, p.y] = glyph.priority
            }
        }
    }

    private fun drawWorld() {
        //TODO: Try drawing whole level and then moving it
        //TODO: Animations
        priority.forEach { it.fill(minPriority) }
        val behaviours = ArrayList<PredictableBehaviour>()
        val playerPos = game.player<Position>()!!
        val stats = game.player<Stats>()!!
        val level = playerPos.level
        moveCamera(playerPos.point)

        //visit fov
        game.player.all<Senses>().forEach { sense ->
            sense.visitFov { entity, x, y ->
                entity.all<PredictableBehaviour>().firstOrNull()?.let { behaviours.add(it) }
                entity<Glyph>()?.let { glyph ->
                    if (glyph.memorable) level.remember(x, y, glyph, game.time)
                    putGlyph(glyph, x, y)
                }
            }
        }

        //draw from memory
        range(ui.worldWidth, ui.worldHeight).forEach { (x, y) ->
            val pos = levelPos(x, y)
            level.memory(pos.x, pos.y)?.let {
                putGlyph(it, pos.x, pos.y, settings.memoryColor)
            } ?: putGlyph(Glyph(Entity.world, ' ', minPriority + 1), pos.x, pos.y)
        }

        //predict
        behaviours.forEach { behaviour ->
            var color = ui.world.fgColor * 0.5
            val prediction =
                level.predictWithGlyph(behaviour, game.player<ThinkUntilSet>()!!.waitTime + stats.speed.toLong())
            prediction.forEach { (p, glyph) ->
                if (game.player.all<Senses>().any { it.isVisible(p.x, p.y) } && inView(p.x, p.y)) {
                    color *= 0.85
                    putGlyph(glyph, p.x, p.y, color)
                }
            }
        }
    }

    override fun onInput(input: Input) {
        ui.info.writeLine("$input", 23)
        if (input == Input.Quit) System.exit(0)
        state = state.handleInput(input)
    }
}