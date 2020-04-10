package gehenna.utils

import gehenna.core.Context
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

class SaveData(context: Context) : Serializable {
    val levels = context.levels
    val player = context.player
    val time = context.time
}

class SaveManager(saveFileName: String) {

    private val saveFile = File(saveFileName)

    fun saveContext(context: Context) {
        ObjectOutputStream(saveFile.outputStream()).use { stream ->
            stream.writeObject(SaveData(context))
        }
        println("DONE!!")
    }

    fun loadContext(): SaveData {
        ObjectInputStream(saveFile.inputStream()).use { stream ->
            return stream.readObject() as SaveData
        }
    }

    fun clean() {
        saveFile.delete()
    }

    fun saveExists(): Boolean {
        return saveFile.exists()
    }
}
