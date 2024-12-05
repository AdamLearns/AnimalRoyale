package live.adamlearns.animalroyale

import live.adamlearns.animalroyale.ComponentUtils.join
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.Title.Times
import net.kyori.adventure.util.Ticks
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.time.Duration

class EventListener(private val gameContext: GameContext) : Listener {
    private val remainingPlayersComponent: TextComponent
        get() {
            val playersWithLivingSheep = gameContext.players.playersWithLivingSheep
            if (playersWithLivingSheep.size > 10) { return Component.text("") }

            val remainingPlayerNames = playersWithLivingSheep.filterNotNull()
                .map { it.colorfulName }
                .let { join(" ", *it.toTypedArray()) }

            return Component.text(": ").append(remainingPlayerNames)
        }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        gameContext.playerJoined(player)
        player.gameMode = GameMode.SPECTATOR
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        // Certain damage causes (e.g. fall) are ignored before the game starts
        if (DAMAGE_CAUSES_IGNORED_OUTSIDE_GAMEPLAY.contains(event.cause) && gameContext.gamePhase != GamePhase.GAMEPLAY) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onEntityDied(event: EntityDeathEvent) {
        if (event.entity.type != EntityType.SHEEP || !event.entity.isCustomNameVisible) {
            return
        }

        // Make sure this was a player sheep that died and not anything else
        val ownerOfDyingSheep = gameContext.getOwnerOfEntity(event.entity) ?: return
        playerDied(ownerOfDyingSheep)

        // Print a message for special death causes
        when (event.entity.lastDamageCause?.cause) {
            EntityDamageEvent.DamageCause.FALL -> {
                val txt1 = ownerOfDyingSheep.colorfulName
                val txt2 = Component.text("fell too far").color(TextColor.color(NamedTextColor.RED))
                gameContext.javaPlugin.server.broadcast(join(" ", txt1, txt2))
                gameContext.twitchChat?.sendMessageToChannel(ownerOfDyingSheep.nameForTwitch + " fell too far admRocket")
            }
            EntityDamageEvent.DamageCause.DROWNING -> {
                val txt1 = ownerOfDyingSheep.colorfulName
                val txt2 = Component.text("drowned").color(TextColor.color(NamedTextColor.DARK_BLUE))
                gameContext.javaPlugin.server.broadcast(join(" ", txt1, txt2))
                gameContext.twitchChat?.sendMessageToChannel(ownerOfDyingSheep.nameForTwitch + " drowned admBoat")
            }
            else -> {}
        }

//        gameContext.getTwitchChat().getTwitchClient().getChat().sendPrivateMessage(ownerOfDyingSheep.getName(), "Your sheep died.");
        val numLivingSheep = gameContext.players.numLivingSheep

        // Only print the number of remaining sheep when it's "meaningful"
        val shouldPrintMessage = numLivingSheep <= 10 || numLivingSheep % 5 == 0
        if (shouldPrintMessage) {
            // This gets called after the entity is already dead, so this message will have the correct number.
            val txt1 = Component.text(numLivingSheep, TextColor.color(NamedTextColor.AQUA))
            val txt2 = Component.text(" sheep remaining")
            gameContext.javaPlugin.server.broadcast(join(txt1, txt2, remainingPlayersComponent))
        }

        // We have a winner!
        if (numLivingSheep == 1) {
            val arena = gameContext.arena ?: return
            arena.cancelSuddenDeath()

            val lastRemainingPlayer = gameContext.players.lastRemainingPlayer ?: return

            val titleText = "${lastRemainingPlayer.name} wins!"
            val subtitle = "#1 of ${arena.startingNumSheep} sheep"
            val times = Times.times(Duration.ofMillis(500), Duration.ofSeconds(10), Duration.ofSeconds(1))
            val title = Title.title(Component.text(titleText), Component.text(subtitle), times)

            gameContext.firstPlayer?.showTitle(title)
            gameContext.twitchChat?.sendMessageToChannel("${lastRemainingPlayer.nameForTwitch} won the battle! PogChamp GG, everyone!")
            gameContext.scheduleArenaReset(15 * Ticks.TICKS_PER_SECOND)
        }
    }

    @EventHandler
    fun onEntityTakeDamageFromOtherEntity(event: EntityDamageByEntityEvent) {
        val damagingEntity = event.damager
        val damagedEntity = event.entity

        // If it's not a sheep we aren't interested
        if (damagedEntity.type != EntityType.SHEEP) {
            return
        }

        // Make sure the sheep was controlled by a player
        if (!damagedEntity.isCustomNameVisible) return

        // Make sure fireworks don't do damage since they're just intended to help players identify themselves
        if (damagingEntity.type == EntityType.FIREWORK_ROCKET) {
            event.isCancelled = true
            return
        }

        // We only care if they died
        if (event.damage < (damagedEntity as LivingEntity).health) {
            return
        }

        val ownerOfDyingSheep = gameContext.getOwnerOfEntity(damagedEntity) ?: return

        var deathMessage: Component? = null
        var twitchDeathMessage: String? = null

        if (event.cause == EntityDamageEvent.DamageCause.LAVA) {
            deathMessage = ownerOfDyingSheep.colorfulName.append(
                Component.text(
                    " was consumed by lava :(",
                    TextColor.color(NamedTextColor.RED)
                )
            )
            twitchDeathMessage = "${ownerOfDyingSheep.nameForTwitch} was consumed by lava admFire"
        } else if (event.cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION && damagingEntity.type == EntityType.TNT) {
            val ownerOfTnt = gameContext.getOwnerOfEntity(damagingEntity) ?: return

            if (ownerOfTnt == ownerOfDyingSheep) {
                deathMessage = ownerOfDyingSheep.colorfulName.append(
                    Component.text(
                        " blasted themselves :(",
                        TextColor.color(NamedTextColor.RED)
                    )
                )
                twitchDeathMessage = "${ownerOfTnt.nameForTwitch} blasted themselves admFire"
            } else {
                val txt1 = ownerOfTnt.colorfulName
                val txt2 = Component.text("blasted")
                val txt3 = ownerOfDyingSheep.colorfulName
                val txt4 = Component.text("to smithereens", TextColor.color(NamedTextColor.RED))
                deathMessage = join(" ", txt1, txt2, txt3, txt4)
                twitchDeathMessage = "${ownerOfTnt.nameForTwitch} blasted ${ownerOfDyingSheep.nameForTwitch} admNuke"

                incrementKillsForPlayer(ownerOfTnt)
            }
        }

        if (twitchDeathMessage != null) {
            gameContext.twitchChat?.sendMessageToChannel(twitchDeathMessage)
        }

        if (deathMessage != null) {
            gameContext.javaPlugin.server.broadcast(deathMessage)
        }
    }

    private fun playerDied(gamePlayer: GamePlayer) {
        Bukkit.getScheduler().runTask(gameContext.javaPlugin) { _ ->
            gameContext.killsScoreboardObjective
                ?.getScore(gamePlayer.name)
                ?.customName(gamePlayer.nameForScoreboardWhenDead)
        }
    }

    private fun incrementKillsForPlayer(gamePlayer: GamePlayer) {
        Bukkit.getScheduler().runTask(gameContext.javaPlugin) { _ ->
            gameContext.killsScoreboardObjective
                ?.getScore(gamePlayer.name)
                ?.let { it.score += 1 }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        gameContext.playerLeft(event.player)
    }

    @EventHandler
    fun onPlayerKick(event: PlayerKickEvent) {
        gameContext.playerLeft(event.player)
    }

    companion object {
        val DAMAGE_CAUSES_IGNORED_OUTSIDE_GAMEPLAY = listOf(
            EntityDamageEvent.DamageCause.FALL,
            EntityDamageEvent.DamageCause.DROWNING
        )
    }
}
