package live.adamlearns.animalroyale

import live.adamlearns.animalroyale.ComponentUtils.join
import live.adamlearns.animalroyale.extensions.cancelIfNeeded
import live.adamlearns.animalroyale.extensions.clamp
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.util.Ticks
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask

class Hud(private val gameContext: GameContext) {
    private val gamePhaseBossBar = BossBar.bossBar(Component.text(), 1f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS)
    private val tntBossBar = BossBar.bossBar(Component.text(), 1f, BossBar.Color.RED, BossBar.Overlay.PROGRESS)

    private var updateTntTask: BukkitTask? = null

    private var gamePhase: GamePhase? = null
    private var gamePhaseStartTick = 0f
    private var gamePhaseEndTick = 1f

    init {
        Bukkit.getScheduler()
            .runTaskTimer(gameContext.javaPlugin, this::updateGamePhaseBar, 0L, PHASE_BAR_UPDATE_PERIOD)
    }

    private fun updateGamePhaseBar() {
        showBarToPlayerIfNeeded(gamePhaseBossBar)

        // When we change game phase, we need to set the bar title and reset the progress
        if (gamePhase != gameContext.gamePhase) {
            gamePhase = gameContext.gamePhase

            val currentTick = gameContext.javaPlugin.server.currentTick
            gamePhaseStartTick = currentTick.toFloat()
            gamePhaseEndTick = (currentTick + getPhaseDuration(gameContext.gamePhase)).toFloat()

            gamePhaseBossBar.name(GamePhaseTitles.getTitle(gameContext.gamePhase))
            gamePhaseBossBar.progress(1f)

            if (gamePhase == GamePhase.GAMEPLAY) {
                updateTntTask =
                    Bukkit.getScheduler()
                        .runTaskTimer(gameContext.javaPlugin, this::updateTntBar, 0L, TNT_BAR_UPDATE_PERIOD)
//                showBarToPlayerIfNeeded(tntBossBar)
            } else {
                updateTntTask?.cancelIfNeeded()
//                hideBarFromPlayerIfNeeded(tntBossBar)
            }
        }

        // Update the progress
        if (PHASES_THAT_HAVE_PROGRESS.contains(gamePhase)) {
            val currentTick = gameContext.javaPlugin.server.currentTick
            val progress: Float =
                ((currentTick.toFloat() - gamePhaseStartTick) / (gamePhaseEndTick - gamePhaseStartTick)).clamp(0f, 1f)
            gamePhaseBossBar.progress(1f - progress) // We want the bar to go 'down' to get the timer effect
        }
    }

    private fun updateTntBar() {
    }

    private fun showBarToPlayerIfNeeded(bar: BossBar) {
        val player = gameContext.firstPlayer ?: return

        if (!player.activeBossBars().contains(bar)) {
            player.showBossBar(bar)
        }
    }

    private fun hideBarFromPlayerIfNeeded(bar: BossBar) {
        val player = gameContext.firstPlayer ?: return

        if (player.activeBossBars().contains(bar)) {
            player.hideBossBar(bar)
        }
    }

    companion object {
        private const val PHASE_BAR_UPDATE_PERIOD: Long = 1L * Ticks.TICKS_PER_SECOND
        private const val TNT_BAR_UPDATE_PERIOD: Long = 5 // ticks

        private val PHASES_THAT_HAVE_PROGRESS = listOf(GamePhase.LOBBY, GamePhase.LOBBY)

        private fun getPhaseDuration(phase: GamePhase): Int =
            when (phase) {
                GamePhase.LOBBY -> Arena.NUM_SECONDS_BEFORE_STARTING_MATCH * Ticks.TICKS_PER_SECOND
                GamePhase.GAMEPLAY -> Arena.NUM_SECONDS_BEFORE_SUDDEN_DEATH * Ticks.TICKS_PER_SECOND
                // For other phases, return an arbitrary long timeframe since we do not update the bar during them
                else -> 3600 * Ticks.TICKS_PER_SECOND
            }
    }

    private object GamePhaseTitles {
        val CREATING_ARENA_TITLE: Component = Component.text("Waiting to start...")

        val LOBBY_TITLE: Component
            get() = join(
                " ",
                Component.text("Game will start soon, type"),
                Component.text("!join", NamedTextColor.BLUE),
                Component.text("in chat!"),
            )

        val PRE_GAMEPLAY_TITLE: Component = Component.text("Starting soon...")

        val GAMEPLAY: Component
            get() = join(
                " ",
                Component.text("Game is ongoing! Waiting for someone to"),
                Component.text("win", NamedTextColor.GREEN),
                Component.text("or for"),
                Component.text("Sudden Death", NamedTextColor.RED),
                Component.text("to start")
            )

        fun getTitle(phase: GamePhase): Component =
            when (phase) {
                GamePhase.CREATING_ARENA -> CREATING_ARENA_TITLE
                GamePhase.PRE_GAMEPLAY -> PRE_GAMEPLAY_TITLE
                GamePhase.LOBBY -> LOBBY_TITLE
                GamePhase.GAMEPLAY -> GAMEPLAY
            }
    }
}
