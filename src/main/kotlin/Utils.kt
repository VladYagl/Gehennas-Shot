import java.awt.Color

operator fun <T> Array<Array<T>>.get(x: Int, y: Int): T {
    return get(x)[y]
}

operator fun <T> Array<Array<T>>.set(x: Int, y: Int, a: T) {
    get(x)[y] = a
}

operator fun Color.times(alpha: Double): Color {
    return Color((red * alpha).toInt(), (green * alpha).toInt(), (blue * alpha).toInt())
}
