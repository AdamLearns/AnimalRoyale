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

    private int tntNextYaw = 0;
    private int tntNextPitch = 0;
    private int tntNextPower = 0;
    private double tntNextTtl = 0;
    private boolean hasSetTntParameters = false;

    // This is essentially measured in System.currentTimeMillis
    private long nextTimeAbleToUseSpecialAbility = 0;

    public static final int MIN_PITCH = -90;
    public static final int MAX_PITCH = 90;
    public static final int MIN_POWER = 0;
    public static final int MAX_POWER = 100;
    public static final double MIN_TTL = 0;
    public static final double MAX_TTL = 5.0;

    public GamePlayer(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setTntParameters(final int yaw, final int pitch, final int power, final double ttl) {
        setYaw(yaw);
        setPitch(pitch);
        setPower(power);
        setTtl(ttl);
        hasSetTntParameters = true;
    }

    public void setYaw(final int yaw) {
        tntNextYaw = yaw;
    }

    public void setPitch(final int pitch) {
        tntNextPitch = Util.clamp(pitch, MIN_PITCH, MAX_PITCH);
    }

    public void setPower(final int power) {
        tntNextPower = Util.clamp(power, MIN_POWER, MAX_POWER);
    }

    public void setTtl(final double ttl) {
        tntNextTtl = Util.clamp(ttl, MIN_TTL, MAX_TTL);
    }

    public void addYaw(final int yaw) {
        setYaw(tntNextYaw + yaw);
    }

    public void addPitch(final int pitch) {
        setPitch(tntNextPitch + pitch);
    }

    public void addPower(final int power) {
        setPower(tntNextPower + power);
    }

    public void addTtl(final double ttl) {
        setTtl(tntNextTtl + ttl);
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

    public String getNameForScoreboardWhenDead() {
        // The order for coloring a struck-out name is COLOR + STRIKETHROUGH, not the other way around.
        return nameColor.toString() + ChatColor.STRIKETHROUGH + name;
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

    public boolean canUseSpecialAbility() {
        return System.currentTimeMillis() >= nextTimeAbleToUseSpecialAbility;
    }

    public void setNextTimeAbleToUseSpecialAbility(final long nextTimeAbleToUseSpecialAbility) {
        this.nextTimeAbleToUseSpecialAbility = nextTimeAbleToUseSpecialAbility;
    }

    public String getNameForTwitch() {
        return "@" + name;
    }

    public String getCooldownMessage() {
        final float secRemaining = (nextTimeAbleToUseSpecialAbility - System.currentTimeMillis()) / 1000.0f;
        return String.format("Your ability is on cooldown for another %.2fs", secRemaining);
    }
}
