operator fun <T> Array<Array<T>>.get(x: Int, y: Int): T {
    return get(x)[y]
}

operator fun <T> Array<Array<T>>.set(x: Int, y: Int, a: T) {
    get(x)[y] = a
}
