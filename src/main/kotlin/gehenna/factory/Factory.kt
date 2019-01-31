package gehenna.factory

import java.io.InputStream

interface Factory<T> {
    fun new(name: String): T
}

interface JsonFactory<T> : Factory<T> {
    fun loadJson(input: Pair<InputStream, String>)
}

