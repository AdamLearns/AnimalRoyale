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

    private int tntNextYaw;
    private int tntNextPitch;
    private int tntNextPower;
    private int tntNextTtl;
    private boolean hasSetTntParameters = false;

    public GamePlayer(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setTntParameters(final int yaw, final int pitch, final int distance, final int ttl) {
        tntNextYaw = yaw;
        tntNextPitch = pitch;
        tntNextPower = distance;
        tntNextTtl = ttl;
        hasSetTntParameters = true;
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

    public int getTntNextYaw() {
        return tntNextYaw;
    }

    public int getTntNextPitch() {
        return tntNextPitch;
    }

    public int getTntNextPower() {
        return tntNextPower;
    }

    public int getTntNextTtl() {
        return tntNextTtl;
    }

    public boolean hasSetTntParameters() {
        return hasSetTntParameters;
    }
}
