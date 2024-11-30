package live.adamlearns.animalroyale.extensions

import java.util.concurrent.ThreadLocalRandom

/**
 * Randomly get a single element from the specified array.
 *
 * @param <T>
 * @return
 */
fun <T> Array<T>.sample(): T {
    val rnd = ThreadLocalRandom.current().nextInt(this.size)
    return this[rnd]
}
