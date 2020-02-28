package gehenna.core

import gehenna.component.Logger
import gehenna.component.Senses
import gehenna.component.behaviour.PlayerBehaviour
import gehenna.factory.Factory
import gehenna.factory.LevelPart
import gehenna.level.Level
import gehenna.level.LevelFactory
import gehenna.utils.SaveData

class Game(override val factory: Factory<Entity>, override val partFactory: Factory<LevelPart>) : Context {
    override lateinit var player: Entity
        private set
    private var globalTime: Long = 0
    override val actionQueue = ActionQueue
    override val time: Long get() = globalTime + (actionQueue.firstOrNull()?.waitTime ?: 0)
    override lateinit var levelFactory: LevelFactory<out Level>
    override val levels = ArrayList<Level>()

    fun init(levelFactory: LevelFactory<out Level>) {
        this.levelFactory = levelFactory
        player = factory.new("player")
//        levelFactory.size = Size(8 * 8, 7 * 8)
        val (level, pos) = levelFactory.new()
        level.spawn(player, pos)
    }

    fun initFromSave(saveData: SaveData, levelFactory: LevelFactory<out Level>) {
        this.levelFactory = levelFactory
        levels.addAll(saveData.levels)
        player = saveData.player
        globalTime = saveData.time
        levels.forEach {
            it.getAll().forEach { entity ->
                entity.any<ActiveComponent>()?.let { activeComponent ->
                    actionQueue.add(activeComponent)
                }
            }
        }
    }

    fun isPlayerNext(): Boolean = actionQueue.firstOrNull() == player<PlayerBehaviour>()

    // TODO: Think about energy randomization / but maybe i don't really need one / you can add this in scale time inside behaviour/characterBehaviour
    suspend fun update() {
        actionQueue.firstOrNull()?.let { first ->
            val time = first.waitTime
            globalTime += time
            actionQueue.toList().forEach {
                it.waitTime -= time
                if (it is Effect && !it.endless) {
                    it.duration -= time
                    if (it.duration <= 0) {
                        it.entity.remove(it)
                        if (it == first) {
                            return
                        }
                    }
                }
            }

            actionQueue.remove(first)
            val result = first.action().perform(this)
            first.lastResult = result
            first.waitTime += result.time
            if (result.addToQueue) actionQueue.add(first)

            val sight = player<Senses.Sight>()
//            sight?.visitFov { _, _ -> } // I needed this to show player that he saw enemy
            result.logEntries.asSequence().filter { entry: LogEntry ->
                player.all<Senses>().any { entry.sense == it::class }
            }.forEach { entry ->
                when (entry.sense) {
                    Senses.Sight::class -> {
                        val pos = entry.position
                        if (pos == null || sight?.isVisible(pos) == true) {
                            player<Logger>()?.add(entry.text)
                        }
                    }
                }
            }
        }
    }
}