package live.adamlearns.animalroyale;

import org.bukkit.entity.Sheep;

/**
 * This is NOT the same as a Minecraft Player (i.e. an org.bukkit.entity.Player). This represents someone from Twitch
 * chat who has typed commands to play the game.
 */
public class GamePlayer {
    /**
     * This is the person's Twitch name.
     */
    private final String name;
    private boolean hasAddedSheep = false;
    private Sheep sheep;

    public GamePlayer(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setSheep(final Sheep sheep) {
        this.sheep = sheep;
        hasAddedSheep = true;
    }

    public boolean canPlaceSheep() {
        return !hasAddedSheep;
    }

    public boolean hasAddedSheep() {
        return hasAddedSheep;
    }

    public Sheep getSheep() {
        return sheep;
    }
}
