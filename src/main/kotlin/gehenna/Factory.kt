package gehenna

import java.io.InputStream

interface Factory<T> {
    fun new(name: String): T
}

interface JsonFactory<T> : Factory<T> {
    fun loadJson(stream: InputStream)
}

