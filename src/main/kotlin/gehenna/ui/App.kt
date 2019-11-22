package gehenna.ui

import gehenna.component.*
import gehenna.component.behaviour.*
import gehenna.core.Action
import gehenna.core.Action.Companion.oneTurn
import gehenna.core.Entity
import gehenna.core.Game
import gehenna.factory.EntityFactory
import gehenna.factory.LevelPartFactory
import gehenna.ui.panel.GehennaPanel
import gehenna.utils.*
import gehenna.utils.Point.Companion.zero
import kotlinx.coroutines.*
import java.awt.Color
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime

class App(private val ui: UI, private val settings: Settings) : InputListener {
    private val minPriority = Int.MIN_VALUE

    private val saver = SaveManager("save.dat")
    private val factory = EntityFactory()
    private val levelFactory = LevelPartFactory(factory)
    private val game = Game(factory, levelFactory)
    private val context = UIContext(game, ui)
    private var state = State.create(context)

    private var time = 0L
    fun start(load: Boolean) {
        GlobalScope.launch(exceptionHandler) {
            ui.loadingWindow("LOADING") {
                println("Loading factories...")
                factory.loadJson(streamResource("data/entities.json"))
                factory.loadJson(streamResource("data/items.json"))
                levelFactory.loadJson(streamResource("data/rooms.json"))

                if (load) {
                    println("Loading levels from save...")
                    game.initFromSave(saver.loadContext())
                } else {
                    println("Creating game levels...")
                    game.init()
                }

                println("Running main loop...")
                game.player<Logger>()?.add("Welcome! " + 3.toChar() + 3.toChar() + 3.toChar())
                val uiJob = launch(exceptionHandler) {
                    uiLoop()
                }
                launch(exceptionHandler) {
                    while (gameLoop(uiJob)) {
                    }
                }
            }
        }
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        ui.printException(exception)
    }

    private suspend fun gameLoop(uiJob: Job): Boolean {
        return withContext(gameContext) {
            //            if (game.isPlayerNext()) saver.saveContext(game)
            game.update()

            when {
                !game.player.has<Position>() -> {
                    ui.addWindow(GehennaPanel(17, 6, context.settings, keyHandler = MenuInput(object : InputListener {
                        override fun onInput(input: Input) = when (input) {
                            Input.Accept, Input.Cancel -> exitProcess(0)
                            else -> false
                        }
                    })).apply {
                        writeLine("RIP ", 1, Alignment.center)
                        writeLine("YOU ARE DEAD", 3, Alignment.center)
                    })
                    uiJob.cancel()
                    uiJob.join()
                    false
                }
                game.player<Position>()?.level?.depth == 2 -> {
                    ui.addWindow(GehennaPanel(19, 4, context.settings, keyHandler = MenuInput(object : InputListener {
                        override fun onInput(input: Input) = when (input) {
                            Input.Accept, Input.Cancel -> exitProcess(0)
                            else -> false
                        }
                    })).apply {
                        writeLine("WE WON ZULUL", 1, Alignment.center)
                    })
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
            val fps = 1000_000_000L / measureNanoTime {
                time = game.time
                withContext(gameContext) {
                    update()
                }
                ui.update()
            }
            ui.info.writeLine("fps=$fps", 0)
        }
    }

    private fun update() {
        updateLog()
        drawWorld()
        updateInfo()
    }

    private fun updateLog() {
        val log = game.player<Logger>()
        log?.log?.let { messages ->
            val temp = log.tempMessage
            ui.updateLog(if (temp != null) messages + temp else messages)
        }
    }

    private val enemies = ArrayList<CharacterBehaviour>()
    private fun updateInfo() { //todo stop hardcoding y coordinate
        ui.info.writeLine("In game time: " + game.time, 1)
        val glyph = game.player.one<Glyph>()
        val pos = game.player.one<Position>()
        val storage = game.player.one<Inventory>()
        ui.info.writeLine("Player glyph = ${glyph.char}|${glyph.priority}", 2)
        ui.info.writeLine("Player position = ${pos.x}, ${pos.y}", 3)
        ui.info.writeLine("Player hp = " + game.player<Health>()?.current, 4)
        ui.info.writeLine("Effects = " + game.player.all<Effect>(), 5)
        ui.info.writeLine("Inventory ${storage.currentVolume}/${storage.maxVolume}", 8, bg = Color.darkGray)
        ui.info.writeLine("Equipped gun: ${storage.gun?.entity}", 9, Alignment.left, bg = Color.darkGray)
        val dice = (storage.gun?.entity?.invoke<Gun>())?.damage
        ui.info.writeLine("Damage: ${dice?.mean?.format(1)}${241.toChar()}${dice?.std?.format(1)} | $dice", 10, Alignment.left, bg = Color.darkGray)
        repeat(9) { i -> ui.info.clearLine(11 + i) }
        storage.contents.forEachIndexed { index, item ->
            ui.info.writeLine(item.entity.toString(), 11 + index)
        }

        repeat(10) { i -> ui.info.clearLine(21 + i) }
        ui.info.writeLine("Enemies", 20, Alignment.center, ui.info.fgColor, Color.darkGray)
        enemies.forEachIndexed { index, enemy ->
            //            val target = MonsterBehaviour::class.safeCast(enemy)?.target?.entity
            val hp = enemy.entity<Health>()
            ui.info.writeLine("${enemy.entity} | ${enemy.waitTime} [${hp?.current} / ${hp?.max}]", 21 + index)
        }

        repeat(10) { i -> ui.info.clearLine(31 + i) }
        ui.info.writeLine("Objects", 30, Alignment.center, ui.info.fgColor, Color.darkGray)
        pos.neighbors.forEachIndexed { index, entity ->
            ui.info.writeLine(entity.toString(), 31 + index)
        }
    }

    private var camera = zero
    private val cameraBound = ui.worldSize.width / 2 - ui.worldSize.width / 5 at ui.worldSize.height / 2 - 3
    private fun moveCamera(playerPos: Point) {
        var x = camera.x
        var y = camera.y
        if (playerPos.x < camera.x + cameraBound.x) {
            x = playerPos.x - cameraBound.x
        }
        if (playerPos.y < camera.y + cameraBound.y) {
            y = playerPos.y - cameraBound.y
        }
        val end = camera + ui.worldSize
        if (playerPos.x > end.x - cameraBound.x) {
            x = playerPos.x + cameraBound.x - ui.worldSize.width
        }
        if (playerPos.y > end.y - cameraBound.y) {
            y = playerPos.y + cameraBound.y - ui.worldSize.height
        }

        camera = x at y
    }

    private fun inView(point: Point): Boolean {
        return point - camera in ui.worldSize
    }

    private fun viewPoint(point: Point): Point {
        return point - camera
    }

    private fun levelPos(point: Point): Point {
        return point + camera
    }

    private val priority = IntArray(ui.worldSize) { minPriority }
    private fun putGlyph(glyph: Glyph, point: Point, fg: Color = ui.world.fgColor, bg: Color = ui.world.bgColor) {
        if (inView(point)) {
            val g = if (cursorShown && cursor.x == point.x && cursor.y == point.y) {
                cursorGlyph
            } else {
                glyph
            }
            val viewPoint = viewPoint(point)
            if (g.priority > priority[viewPoint]) {
                ui.world.putChar(g.char, viewPoint.x, viewPoint.y, fg, bg)
                priority[viewPoint] = g.priority
            }
        }
    }

    private fun drawWorld() {
        //TODO: Try drawing whole level and then moving it
        //TODO: Animations
        priority.forEach { it.fill(minPriority) }
        enemies.clear()
        val behaviours = ArrayList<PredictableBehaviour<Any>>()
        val playerPos = game.player.one<Position>()
        val playerBehaviour = game.player.one<PlayerBehaviour>()
        val level = playerPos.level
        moveCamera(playerPos)

        //visit fov //todo: if I add hearing this should not draw from it
        game.player.all<Senses>().forEach { sense ->
            sense.visitFov { entity, point ->
                entity.any<PredictableBehaviour<Any>>()?.let { behaviours.add(it) }
                if (entity != game.player)
                    entity.any<CharacterBehaviour>()?.let { enemies.add(it) }
                entity<Glyph>()?.let { glyph ->
                    if (glyph.memorable) level.remember(point, glyph, game.time)
                    putGlyph(glyph, point)
                }
            }
        }

        //draw from memory
        ui.worldSize.range.forEach { point ->
            val pos = levelPos(point)
            level.memory(pos)?.let {
                putGlyph(it, pos, settings.memoryColor)
            } ?: putGlyph(Glyph(Entity.world, ' ', minPriority + 1), pos)
        }

        //predict
        behaviours.forEach { behaviour ->
            var color = ui.world.fgColor * 0.5
            level.predictWithGlyph(behaviour, Behaviour.scaleTime(oneTurn, playerBehaviour.speed))
                    .asSequence()
                    .filter { (pos, _) -> game.player.all<Senses>().any { it.isVisible(pos) } && inView(pos) }
                    .forEach { (pos, glyph) ->
                        color *= 0.85
                        putGlyph(glyph, pos, max(color, Color(40, 40, 40))) //todo constant
                    }
        }
    }

    override fun onInput(input: Input): Boolean {
        if (input == Input.Quit) {
            GlobalScope.launch(exceptionHandler) {
                ui.loadingWindow("SAVING") {
                    saver.saveContext(game)
                }
                exitProcess(0)
            }
        }
        val (newState, consumed) = state.handleInput(input)
        state = newState
        return consumed
    }

    private var cursor: Point = 0 at 0
    private var cursorShown: Boolean = false
    private val cursorGlyph = Glyph(Entity.world, 'X', 1000_000)

    fun showCursor() {
        cursorShown = true
    }

    fun hideCursor() {
        cursorShown = false
    }

    fun setCursor(point: Point) {
        cursor = point
    }
}