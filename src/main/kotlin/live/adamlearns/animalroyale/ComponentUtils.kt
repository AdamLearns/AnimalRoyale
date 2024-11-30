package live.adamlearns.animalroyale

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent

object ComponentUtils {
    /**
     * Merges multiple TextComponents into one.
     */
    @JvmStatic
    fun join(vararg components: TextComponent): TextComponent {
        // The "reset" color, as in the color that's used if nothing is specified, is that of the first TextComponent
        // in the chain, which is why we start with a blank one.
        var result = Component.empty()
        for (component in components) {
            result = result.append(component)
        }
        return result
    }
}
