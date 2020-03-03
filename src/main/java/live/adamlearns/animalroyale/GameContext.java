package live.adamlearns.animalroyale;

import org.bukkit.DyeColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        final String validDyeColors = Stream.of(DyeColor.values()).map(s -> s.toString().toLowerCase()).collect(Collectors.joining(" "));
        twitchChat.sendMessageToChannel("You may now join the game with !join COLOR, where COLOR is one of these: " + validDyeColors);
        gamePhase = GamePhase.LOBBY;
    }

    public void advanceGamePhaseToPreGameplay() {
        final int numPlayers = players.getAllPlayers().size();
        twitchChat.sendMessageToChannel("The battle is about to start with " + numPlayers + " players! Make sure to whisper your commands with \"/w AdamLearnsBot COMMAND\"");
        gamePhase = GamePhase.PRE_GAMEPLAY;
    }

    public void advanceGamePhaseToGameplay() {
        final int numPlayers = players.getAllPlayers().size();
        arena.setStartingNumSheep(players.getNumLivingSheep());
        twitchChat.sendMessageToChannel("The battle is starting with " + numPlayers + " players! Type !tnt YAW PITCH POWER TTL to engage in battle! More help here: https://imgur.com/XMui9vf.png");
        gamePhase = GamePhase.GAMEPLAY;
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
            firstPlayer.setFlySpeed(0.6f);

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
