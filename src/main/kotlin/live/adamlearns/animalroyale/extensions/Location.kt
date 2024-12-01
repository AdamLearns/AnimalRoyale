package live.adamlearns.animalroyale.extensions

import org.bukkit.Location
import kotlin.math.floor

/**
 * Locations that are at the edge of a block may cause entities to suffocate in nearby blocks. To fix this, you
 * apparently just pick the center of the block as mentioned <a href="https://www.spigotmc.org/threads/teleporting-player-to-center-of-a-block.255699/?__cf_chl_jschl_tk__=4e2c930c0179625dd750d46b94c5c116f2d94707-1583359206-0-AWv8EYIRfuycEC86A">here</a>
 */
fun Location.setToCenterOfBlock() {
    this.x = floor(this.x) + 0.5
    this.z = floor(this.z) + 0.5
}

/**
 * Gets the X coordinate of the western boundary of the arena.
 */
fun Location.westX(depth: Int): Int {
    return blockX - depth
}

/**
 * Gets the X coordinate of the eastern boundary of the arena.
 */
fun Location.eastX(depth: Int): Int {
    return blockX + depth
}

/**
 * Gets the Z coordinate of the northern boundary of the arena.
 */
fun Location.northZ(depth: Int): Int {
    return blockZ
}

/**
 * Gets the Z coordinate of the southern boundary of the arena.
 */
fun Location.southZ(depth: Int): Int {
    return blockZ + depth
}

