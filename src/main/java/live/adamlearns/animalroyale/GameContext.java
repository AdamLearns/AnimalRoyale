package live.adamlearns.animalroyale;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.Set;
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
    private Objective killsScoreboardObjective;
    private BukkitTask resetArenaTask = null;

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
        twitchChat.sendMessageToChannel("You may now join the game with \"/w AdamLearnsBot !join COLOR\", where COLOR is one of these: " + validDyeColors);
        gamePhase = GamePhase.LOBBY;
    }

    /**
     * This will reset ALL state so that people can join a brand new world.
     */
    public void startNewGame() {
        gamePhase = GamePhase.PRE_GAMEPLAY;

        // Regardless of whether this was called manually or automatically, make sure that we don't have the automatic
        // task scheduled or else we'll reset a second time.
        if (resetArenaTask != null && !resetArenaTask.isCancelled()) {
            resetArenaTask.cancel();
            resetArenaTask = null;
        }

        twitchChat.sendMessageToChannel("Setting up for a brand new game.");

        resetEntireScoreboard();

        // This needs to happen before we remove all of the players so that we can find and remove their sheep.
        this.createNewArena();

        // If we ever want persistent stats, then instead of just wiping out all players, you'd need to reset their
        // individual state (basically just TNT- and sheep-related state).
        players.removeAllPlayers();
    }

    private void resetEntireScoreboard() {
        if (killsScoreboardObjective == null) {
            return;
        }

        final Scoreboard scoreboard = killsScoreboardObjective.getScoreboard();
        if (scoreboard != null) {
            final Set<String> scoreboardEntries = scoreboard.getEntries();
            for (final String scoreboardEntry :
                    scoreboardEntries) {
                scoreboard.resetScores(scoreboardEntry);
            }
        }
    }

    public void advanceGamePhaseToPreGameplay() {
        final int numPlayers = players.getAllPlayers().size();
        twitchChat.sendMessageToChannel("The battle is about to start with " + numPlayers + " players! Make sure to whisper your commands with \"/w AdamLearnsBot COMMAND\"");
        gamePhase = GamePhase.PRE_GAMEPLAY;

        // Now that people can no longer join, we can set up the scoreboard
        setUpScoreboard();
    }

    private void setUpScoreboard() {
        Bukkit.getScheduler().runTask(javaPlugin, x -> {
            final ScoreboardManager manager = Bukkit.getScoreboardManager();

            final Scoreboard board = manager.getNewScoreboard();
            killsScoreboardObjective = board.registerNewObjective("Kills", "dummy", "Kills");
            killsScoreboardObjective.setDisplaySlot(DisplaySlot.SIDEBAR);

            for (final GamePlayer gamePlayer :
                    players.getAllPlayers().values()) {
                final Score score = killsScoreboardObjective.getScore(gamePlayer.getNameColoredForInGameChat());
                score.setScore(0); //Integer only!
            }

            firstPlayer.setScoreboard(board);
        });
    }

    public void advanceGamePhaseToGameplay() {
        final int numPlayers = players.getAllPlayers().size();
        arena.setStartingNumSheep(players.getNumLivingSheep());
        twitchChat.sendMessageToChannel("The battle is starting with " + numPlayers + " players! Type \"/w AdamLearnsBot !tnt YAW PITCH POWER TTL\" to engage in battle! More help here: https://imgur.com/XMui9vf.png");
        gamePhase = GamePhase.GAMEPLAY;
    }

    public boolean canAddSheep() {
        return arena != null && gamePhase == GamePhase.LOBBY;
    }

    public void createNewArena() {
        if (arena != null) {
            arena.dispose();
        }
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
            firstPlayer.setFlySpeed(0.35f);

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

    public TwitchChat getTwitchChat() {
        return twitchChat;
    }

    public Objective getKillsScoreboardObjective() {
        return killsScoreboardObjective;
    }

    /**
     * Schedules a new game to be started in the future.
     *
     * @param delayInTicks
     */
    public void scheduleArenaReset(final int delayInTicks) {
        // Don't allow scheduling multiple resets
        if (resetArenaTask != null) {
            return;
        }
        resetArenaTask = Bukkit.getScheduler().runTaskLater(javaPlugin, this::startNewGame, delayInTicks);
    }
}
