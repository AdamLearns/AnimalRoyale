package live.adamlearns.animalroyale.extensions

fun <T : Comparable<T>> T.clamp(min: T, max: T): T {
    return if (this < min) min else if (this > max ) max else this
}
