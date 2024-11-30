package live.adamlearns.animalroyale;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public class MessageUtil {

    /**
     * Merges multiple TextComponents into one. The "reset" color, as in the color that's used if nothing is specified,
     * is that of the first TextComponent in the chain, which is why we start with a blank one.
     */
    public static TextComponent MergeTextComponents(TextComponent... components) {
        TextComponent result = Component.empty();
        for (TextComponent component : components) {
            result = result.append(component);
        }
        return result;
    }
}
