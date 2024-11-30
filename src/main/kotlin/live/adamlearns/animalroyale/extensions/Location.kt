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
