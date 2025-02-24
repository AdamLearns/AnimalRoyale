package live.adamlearns.animalroyale

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Bukkit
import org.bukkit.DyeColor
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import java.util.*

/**
 * This keeps track of all classes needed in order for the plug-in to work.
 */
class GameContext(
    @JvmField
    val javaPlugin: JavaPlugin
) {
    @JvmField
    val players: Players = Players()

    var gamePhase: GamePhase = GamePhase.CREATING_ARENA
        private set
    var world: World? = null
        private set
    var twitchChat: TwitchChat? = null
        private set
    var arena: Arena? = null
        private set
    var killsScoreboardObjective: Objective? = null
        private set
    // The first player is like the "camera" in this game. It's what the streamer on Twitch would be controlling.
    var firstPlayer: Player? = null
        private set

    private var resetArenaTask: BukkitTask? = null

    fun advanceGamePhaseToLobby() {
        javaPlugin.logger.info("Transitioned to LOBBY phase")

        val validDyeColors = DyeColor.values().joinToString(" ") {
            it.toString().lowercase(Locale.getDefault())
        }
        twitchChat?.sendMessageToChannel("You may now join the game with \"!join COLOR\", where COLOR is one of these: $validDyeColors. See https://i.imgur.com/42cQes4.png for how to play.")
        gamePhase = GamePhase.LOBBY
    }

    /**
     * This will reset ALL state so that people can join a brand new world.
     */
    fun startNewGame() {
        gamePhase = GamePhase.PRE_GAMEPLAY

        // Regardless of whether this was called manually or automatically, make sure that we don't have the automatic
        // task scheduled or else we'll reset a second time.
        resetArenaTask?.let { task ->
            if (!task.isCancelled) {
                task.cancel()
                resetArenaTask = null
            }
        }

        twitchChat?.sendMessageToChannel("Setting up for a brand new game.")

        resetEntireScoreboard()

        // This needs to happen before we remove all of the players so that we can find and remove their sheep.
        this.createNewArena()

        // If we ever want persistent stats, then instead of just wiping out all players, you'd need to reset their
        // individual state (basically just TNT- and sheep-related state).
        players.removeAllPlayers()
    }

    private fun resetEntireScoreboard() {
        killsScoreboardObjective?.scoreboard?.let { scoreboard ->
            scoreboard.entries.forEach { scoreboard.resetScores(it) }
        }
    }

    fun advanceGamePhaseToPreGameplay() {
        twitchChat?.sendMessageToChannel("The battle is about to start with ${players.allPlayers.size} players! Get your TNT ready! 🧨")
        gamePhase = GamePhase.PRE_GAMEPLAY

        // Now that people can no longer join, we can set up the scoreboard
        setUpScoreboard()
    }

    private fun setUpScoreboard() {
        Bukkit.getScheduler().runTask(javaPlugin) { _ ->
            val manager = Bukkit.getScoreboardManager()
            val board = manager.newScoreboard
            killsScoreboardObjective = board.registerNewObjective(
                "Kills",
                Criteria.DUMMY,
                Component.text("Kills")
            )
            killsScoreboardObjective?.displaySlot = DisplaySlot.SIDEBAR

            for (gamePlayer in players.allPlayers.values.filterNotNull()) {
                killsScoreboardObjective?.getScore(gamePlayer.name)?.let { score ->
                    score.customName(gamePlayer.colorfulName)
                    score.score = 0 //Integer only!
                }
            }
            firstPlayer?.scoreboard = board
        }
    }

    fun advanceGamePhaseToGameplay() {
        arena?.startingNumSheep = players.numLivingSheep
        twitchChat?.sendMessageToChannel("The battle is starting with ${players.allPlayers.size} players! See how to play here: https://i.imgur.com/42cQes4.png")
        gamePhase = GamePhase.GAMEPLAY
    }

    fun canAddSheep(): Boolean {
        return arena != null && gamePhase == GamePhase.LOBBY
    }

    fun registerWorld(world: World?) {
        this.world = world
    }

    fun registerTwitchChat(twitchChat: TwitchChat?) {
        this.twitchChat = twitchChat
    }

    fun playerLeft(player: Player?) {
        // If the player was the only one in the server, then we have no "firstPlayer" anymore.
        //
        // TODO: this should really just always be the player who joined least recently
        if (world?.playerCount == 1) {
            firstPlayer = null
        }
    }

    fun playerJoined(player: Player?) {
        if (firstPlayer == null) {
            firstPlayer = player
            firstPlayer?.flySpeed = 0.35f

            if (arena == null) {
                this.createNewArena()
            }
        }
    }

    fun getOwnerOfEntity(entity: Entity): GamePlayer? {
        val name = (entity.customName() as TextComponent?)?.content() ?: return null
        return players.getPlayer(name)
    }

    /**
     * Schedules a new game to be started in the future.
     *
     * @param delayInTicks
     */
    fun scheduleArenaReset(delayInTicks: Int) {
        // Don't allow scheduling multiple resets
        if (resetArenaTask != null) {
            return
        }

        resetArenaTask = Bukkit.getScheduler().runTaskLater(
            javaPlugin,
            Runnable { this.startNewGame() },
            delayInTicks.toLong()
        )
    }

    private fun createNewArena() {
        arena?.dispose()
        arena = Arena(this)
    }
}
