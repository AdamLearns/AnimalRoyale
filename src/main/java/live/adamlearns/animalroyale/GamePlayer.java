package live.adamlearns.animalroyale;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
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

    /**
     * This is as close to your sheep's color as we can get. We approximate some of them like black so that it's still
     * readable.
     */
    private ChatColor nameColor;

    private int tntNextYaw;
    private int tntNextPitch;
    private int tntNextPower;
    private double tntNextTtl;
    private boolean hasSetTntParameters = false;

    public GamePlayer(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setTntParameters(final int yaw, final int pitch, final int distance, final double ttl) {
        tntNextYaw = yaw;
        tntNextPitch = pitch;
        tntNextPower = distance;
        tntNextTtl = ttl;
        hasSetTntParameters = true;
    }

    public void setSheep(final Sheep sheep) {
        this.sheep = sheep;
        hasAddedSheep = true;

        final DyeColor sheepColor = sheep.getColor();

        assert sheepColor != null;
        nameColor = GamePlayer.getChatColorFromDyeColor(sheepColor);
    }

    public String getNameColoredForInGameChat() {
        return nameColor + name;
    }

    static ChatColor getChatColorFromDyeColor(final DyeColor dyeColor) {
        switch (dyeColor) {
            case BLACK:
            case LIGHT_GRAY:
                return ChatColor.GRAY;
            case BROWN:
            case ORANGE:
                return ChatColor.GOLD;
            case CYAN:
            case LIGHT_BLUE:
                return ChatColor.AQUA;
            case LIME:
                return ChatColor.GREEN;
            case GREEN:
                return ChatColor.DARK_GREEN;
            case MAGENTA:
            case PINK:
                return ChatColor.LIGHT_PURPLE;
            case PURPLE:
                return ChatColor.DARK_PURPLE;
            default:
                return ChatColor.valueOf(dyeColor.name());
        }
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

    public boolean isSheepAlive() {
        return hasAddedSheep && sheep.isValid() && !sheep.isDead();
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

    public double getTntNextTtl() {
        return tntNextTtl;
    }

    public boolean hasSetTntParameters() {
        return hasSetTntParameters;
    }
}
