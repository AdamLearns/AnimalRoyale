package live.adamlearns.animalroyale.extensions

import java.util.concurrent.ThreadLocalRandom

/**
 * @return Either 1 or -1, chosen randomly.
 */
fun randomSign(): Int = if (ThreadLocalRandom.current().nextBoolean()) -1 else 1
