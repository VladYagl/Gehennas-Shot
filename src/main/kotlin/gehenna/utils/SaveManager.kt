package gehenna.utils

import gehenna.core.Context
import gehenna.core.Entity
import gehenna.level.Level
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

data class SaveData(
        val levels: List<Level>,
        val player: Entity,
        val time: Long
): Serializable

class SaveManager(saveFileName: String) {

    private val saveFile = File(saveFileName)

    fun saveContext(context: Context) {
        ObjectOutputStream(saveFile.outputStream()).use { stream ->
            stream.writeObject(SaveData(context.levels, context.player, context.time))
        }
        println("DONE!!")
    }

    fun loadContext(): SaveData {
        ObjectInputStream(saveFile.inputStream()).use { stream ->
            return stream.readObject() as SaveData
        }
    }
}
