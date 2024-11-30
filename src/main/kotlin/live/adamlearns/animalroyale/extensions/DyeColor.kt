package live.adamlearns.animalroyale.extensions

import net.kyori.adventure.text.format.TextColor
import org.bukkit.DyeColor

fun DyeColor.asTextColor(): TextColor {
    // Override for visibility
    if (this == DyeColor.BLACK) {
        return TextColor.color(128, 128, 128)
    }

    return TextColor.color(color.asRGB())
}
