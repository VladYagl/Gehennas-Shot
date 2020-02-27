package gehenna.ui

import gehenna.component.*
import gehenna.component.behaviour.Behaviour
import gehenna.component.behaviour.CharacterBehaviour
import gehenna.component.behaviour.PlayerBehaviour
import gehenna.component.behaviour.PredictableBehaviour
import gehenna.core.Action.Companion.oneTurn
import gehenna.core.Entity
import gehenna.core.Game
import gehenna.factory.EntityFactory
import gehenna.factory.LevelPartFactory
import gehenna.level.DungeonLevelFactory
import gehenna.level.StubLevelFactory
import gehenna.ui.panel.GehennaPanel
import gehenna.utils.*
import gehenna.utils.Point.Companion.zero
import kotlinx.coroutines.*
import java.awt.Color
import kotlin.math.PI
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime

class App(private val ui: UI, private val settings: Settings) : InputListener {
    private val minPriority = Int.MIN_VALUE

    private val saver = SaveManager("save.dat")
    private val factory = EntityFactory()
    private val levelPartFactory = LevelPartFactory(factory)
    private val game = Game(factory, levelPartFactory)
    private val context = UIContext(game, ui)
    private var state = State.create(context)

    private var time = 0L
    fun start(load: Boolean, level: Int) {
        GlobalScope.launch(exceptionHandler) {
            ui.loadingWindow("LOADING") {
                println("Loading factories...")
                factory.loadJson(streamResource("data/entities.json"))
                factory.loadJson(streamResource("data/items.json"))
                levelPartFactory.loadJson(streamResource("data/rooms.json"))

                val levelFactory = if (level == 0) {
                    DungeonLevelFactory(game).also { it.size = Size(8 * 8, 7 * 8) }
                } else {
                    StubLevelFactory(game).also { it.size = Size(8 * 8, 7 * 8) }
                }


                if (load) {
                    println("Loading levels from save...")
                    game.initFromSave(saver.loadContext(), levelFactory)
                } else {
                    println("Creating game levels...")
                    game.init(levelFactory)
                }

                println("Running main loop...")
                val heart = 3.toChar()
                game.player<Logger>()?.add(_fg("love", "$heart $heart $heart") + " Welcome! " +
                        _fg("love", "$heart $heart $heart"))
                initInfo()
                val uiJob = launch(exceptionHandler) {
                    uiLoop()
                }
                launch(exceptionHandler) {
                    @Suppress("ControlFlowWithEmptyBody")
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
//                game.player<Position>()?.level?.depth == 2 -> {
//                    ui.addWindow(GehennaPanel(19, 4, context.settings, keyHandler = MenuInput(object : InputListener {
//                        override fun onInput(input: Input) = when (input) {
//                            Input.Accept, Input.Cancel -> exitProcess(0)
//                            else -> false
//                        }
//                    })).apply {
//                        writeLine("WE WON ZULUL", 1, Alignment.center)
//                    })
//                    uiJob.cancel()
//                    uiJob.join()
//                    false
//                }
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
            fpsText.line = "fps=$fps"
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

    private val fpsText = TextItem()
    private val timeText = TextItem()
    private val hpText = TextItem()
    private val effectsText = TextItem()
    private val inventoryText = TextItem(bg = Color.darkGray, alignment = Alignment.center)
    private val gunText = TextItem(bg = Color.darkGray)
    private val ammoText = TextItem(bg = Color.darkGray)
    private val dmgText = TextItem(bg = Color.darkGray)
    private val spreadText = TextItem(bg = Color.darkGray)
    private val itemsList = List(10) { TextItem() }
    private val enemiesText = TextItem("Enemies", ui.info.fgColor, Color.darkGray, Alignment.center)
    private val enemiesList = List(5) { TextItem() }
    private val objectsText = TextItem("Objects", ui.info.fgColor, Color.darkGray, Alignment.center)
    private val objectsList = List(5) { TextItem() }

    private fun initInfo() {
        ui.info.addItem(fpsText)
        ui.info.addItem(timeText)
        ui.info.addItem(hpText)
        ui.info.addItem(effectsText)
        ui.info.addItem(inventoryText)
        ui.info.addItem(gunText)
        ui.info.addItem(ammoText)
        ui.info.addItem(dmgText)
        ui.info.addItem(spreadText)
        itemsList.forEach { ui.info.addItem(it) }
        ui.info.addItem(enemiesText)
        enemiesList.forEach { ui.info.addItem(it) }
        ui.info.addItem(objectsText)
        objectsList.forEach { ui.info.addItem(it) }
    }

    private val enemies = ArrayList<CharacterBehaviour>()
    private fun updateInfo() {
        timeText.line = "in game time: " + game.time
        val pos = game.player.one<Position>()
        val storage = game.player.one<Inventory>()
        val hand = game.player.one<MainHandSlot>()
        hpText.line = "HP : " + game.player<Health>()?.current + " / " + game.player<Health>()?.max
        effectsText.line = "Effects = " + game.player.all<Effect>().filterNot { it is PassiveHeal }
        inventoryText.line = "Inventory ${storage.currentVolume}/${storage.maxVolume}"
        gunText.line = "Equipped gun: ${hand.gun?.entity}"
        val gun = hand.gun
        val dice = gun?.fullDamage ?: Dice.Const(0)
        ammoText.line = "${195.toChar()}--Ammo: ${gun?.magazine?.size} / ${gun?.magazine?.capacity}"
        dmgText.line = "${195.toChar()}--${dice.mean.format(1)}${241.toChar()}${dice.std.format(1)} | $dice"
        spreadText.line = "${195.toChar()}--Spread: ${((gun?.spread ?: 0.0) / PI * 180).format(0)}${248.toChar()}"

        itemsList.forEach { it.line = "" }
        storage.stacks.take(10).forEachIndexed { index, item ->
            itemsList[index].line = item.entity.toString()
        }
        //TODO: take 5 - is not perfect

        enemiesList.forEach { it.line = "" }
        enemies.take(5).forEachIndexed { index, enemy ->
            val hp = enemy.entity<Health>()
            enemiesList[index].line = "${enemy.entity} | ${enemy.waitTime} [${hp?.current} / ${hp?.max}]"
        }

        objectsList.forEach { it.line = "" }
        pos.neighbors.packEntities().take(5).forEachIndexed { index, entity ->
            objectsList[index].line = entity.toString()
        }

        ui.info.update()
    }

    private var camera = zero
    private val cameraBound = 15 at 15

    private var followPlayer: Boolean = true
    private var focus: Point = 0 at 0
    fun moveFocus(point: Point) {
        followPlayer = false
        focus = point
    }

    fun focusPlayer() {
        followPlayer = true
    }

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
        if (playerPos.x >= end.x - cameraBound.x) {
            x = playerPos.x + cameraBound.x - ui.worldSize.width + 1
        }
        if (playerPos.y >= end.y - cameraBound.y) {
            y = playerPos.y + cameraBound.y - ui.worldSize.height + 1
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

    fun putCharOnHUD(char: Char, x: Int, y: Int, fg: Color, bg: Color) {
        val point = x at y
        if (inView(point)) {
            val viewPoint = viewPoint(point)
            ui.hud.putChar(char, viewPoint.x, viewPoint.y, fg, bg)
        }
    }

    private fun drawWorld() {
        //TODO: Animations
        priority.forEach { it.fill(minPriority) }
        enemies.clear()
        val behaviours = ArrayList<PredictableBehaviour<Any>>()
        val playerPos = game.player.one<Position>()
        val playerBehaviour = game.player.one<PlayerBehaviour>()
        val level = playerPos.level
        if (followPlayer) {
            moveCamera(playerPos)
        } else {
            moveCamera(focus)
        }

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

        ui.hud.forEachTile { x, y, data ->
            if (data.character != EMPTY_CHAR) {
                ui.world.putChar(data.character, x, y, fg = data.fgColor, bg = data.bgColor)
            } else if (data.bgColor != ui.hud.bgColor) {
                ui.world.changeColors(x, y, data.fgColor, data.bgColor)
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
}