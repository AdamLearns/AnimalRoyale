package live.adamlearns.animalroyale

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.TextComponent

object ComponentUtils {
    /**
     * Merges multiple TextComponents into one.
     */
    @JvmStatic
    fun join(vararg components: TextComponent): Component {
        return join("", *components)
    }

    /**
     * Merges multiple TextComponents into one.
     */
    @JvmStatic
    fun join(separator: String, vararg components: TextComponent): Component {
        return Component.join(JoinConfiguration.separator(Component.text(separator)), *components)
    }
}
