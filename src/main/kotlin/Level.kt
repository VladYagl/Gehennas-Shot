class Level(val width: Int, val height: Int) {
    val cells = Array(width) { Array(height) { HashSet<Entity>() } }
}