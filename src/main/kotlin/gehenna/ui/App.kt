package gehenna.ui

import gehenna.component.*
import gehenna.component.behaviour.CharacterBehaviour
import gehenna.component.behaviour.MonsterBehaviour
import gehenna.component.behaviour.PlayerBehaviour
import gehenna.component.behaviour.PredictableBehaviour
import gehenna.core.Entity
import gehenna.core.Game
import gehenna.factory.EntityFactory
import gehenna.factory.LevelPartFactory
import gehenna.level.DungeonLevelBuilder
import gehenna.utils.*
import gehenna.utils.Point.Companion.zero
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

//        game.player<Logger>()?.add("Welcome to Gehenna's Shot")
        game.player<Logger>()?.add("Welcome! " + 3.toChar() + 3.toChar() + 3.toChar())
        val uiJob = GlobalScope.launch(exceptionHandler) {
            uiLoop()
        }
        GlobalScope.launch(exceptionHandler) {
            while (gameLoop(uiJob)) {
            }
        }
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
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

    private val enemies = ArrayList<CharacterBehaviour>()
    private fun updateInfo() {
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
        repeat(10) { i -> ui.info.clearLine(10 + i) }
        storage.items().forEachIndexed { index, item ->
            ui.info.writeLine(item.entity.toString(), 10 + index)
        }

        repeat(10) { i -> ui.info.clearLine(21 + i) }
        ui.info.writeLine("Enemies", 20, Alignment.center, ui.info.fgColor, Color.darkGray)
        enemies.forEachIndexed { index, enemy ->
            val target = MonsterBehaviour::class.safeCast(enemy)?.target?.entity
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
            val viewPoint = viewPoint(point)
            if (glyph.priority > priority[viewPoint]) {
                ui.world.putChar(glyph.char, viewPoint.x, viewPoint.y, fg, bg)
                priority[viewPoint] = glyph.priority
            }
        }
    }

    private fun drawWorld() {
        //TODO: Try drawing whole level and then moving it
        //TODO: Animations
        priority.forEach { it.fill(minPriority) }
        enemies.clear()
        val behaviours = ArrayList<PredictableBehaviour>()
        val playerPos = game.player.one<Position>()
        val playerBehaviour = game.player.one<PlayerBehaviour>()
        val level = playerPos.level
        moveCamera(playerPos)

        //visit fov
        game.player.all<Senses>().forEach { sense ->
            sense.visitFov { entity, point ->
                entity.any<PredictableBehaviour>()?.let { behaviours.add(it) }
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
            val prediction =
                    level.predictWithGlyph(behaviour, playerBehaviour.waitTime + playerBehaviour.speed.toLong())
            prediction.forEach { (pos, glyph) ->
                if (game.player.all<Senses>().any { it.isVisible(pos) } && inView(pos)) {
                    color *= 0.85
                    putGlyph(glyph, pos, max(color, Color(40, 40, 40))) //todo constant
                }
            }
        }
    }

    override fun onInput(input: Input) {
        if (input == Input.Quit) System.exit(0)
        state = state.handleInput(input)
    }
}