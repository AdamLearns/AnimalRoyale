package live.adamlearns.animalroyale;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This keeps track of all classes needed in order for the plug-in to work.
 */
public class GameContext {
    private GamePhase gamePhase;
    private World world;
    private TwitchChat twitchChat;
    private Arena arena;
    private final JavaPlugin javaPlugin;
    private final Players players;

    // The first player is like the "camera" in this game. It's what the streamer on Twitch would be controlling.
    private Player firstPlayer;

    public GameContext(final JavaPlugin javaPlugin) {
        gamePhase = GamePhase.CREATING_ARENA;

        players = new Players();

        this.javaPlugin = javaPlugin;
    }

    public void advanceGamePhaseToLobby() {
        javaPlugin.getLogger().info("Transitioned to LOBBY phase");
        gamePhase = GamePhase.LOBBY;
    }

    public boolean canAddSheep() {
        return arena != null && gamePhase == GamePhase.LOBBY;
    }

    public void createNewArena() {
        arena = new Arena(this);
    }

    public void registerWorld(final World world) {
        this.world = world;
    }

    public void registerTwitchChat(final TwitchChat twitchChat) {
        this.twitchChat = twitchChat;
    }

    public World getWorld() {
        return world;
    }

    public void playerLeft(final Player player) {
        // If the player was the only one in the server, then we have no "firstPlayer" anymore.
        //
        // TODO: this should really just always be the player who joined least recently
        if (world.getPlayerCount() == 1) {
            firstPlayer = null;
        }
    }

    public void playerJoined(final Player player) {
        if (firstPlayer == null) {
            firstPlayer = player;

            if (arena == null) {
                this.createNewArena();
            }
        }
    }

    public Player getFirstPlayer() {
        return firstPlayer;
    }

    public Arena getArena() {
        return arena;
    }

    public JavaPlugin getJavaPlugin() {
        return javaPlugin;
    }

    public Players getPlayers() {
        return players;
    }

    public GamePhase getGamePhase() {
        return gamePhase;
    }
}
