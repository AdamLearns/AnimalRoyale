package live.adamlearns.animalroyale;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

import java.util.List;
import java.util.Objects;

public class EventListener implements Listener {

    private final GameContext gameContext;

    public EventListener(final GameContext gameContext) {
        this.gameContext = gameContext;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        gameContext.playerJoined(player);
        player.setGameMode(GameMode.SPECTATOR);
    }

    @EventHandler
    public void onEntityDamage(final EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {

            if (gameContext.getGamePhase() == GamePhase.LOBBY || gameContext.getGamePhase() == GamePhase.PRE_GAMEPLAY) {
                // Prevent the fall animation from happening since no damage is being taken
                event.setCancelled(true);
            }

            if (gameContext.getGamePhase() == GamePhase.GAMEPLAY) {
                final LivingEntity entity = (LivingEntity) event.getEntity();
                if (entity.getType() == EntityType.SHEEP && event.getDamage() >= entity.getHealth()) {
                    final String sheepName = entity.getCustomName();
                    final GamePlayer ownerOfSheep = gameContext.getPlayers().getPlayer(sheepName);
                    if (ownerOfSheep != null) {
                        TextComponent txt1 = ownerOfSheep.getColorfulName();
                        TextComponent txt2 = Component.text(" fell too far").color(TextColor.color(NamedTextColor.RED));
                        gameContext.getJavaPlugin().getServer().broadcast(txt1.append(txt2));
                        gameContext.getTwitchChat().sendMessageToChannel(ownerOfSheep.getNameForTwitch() + " fell too far admRocket");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDied(final EntityDeathEvent event) {
        final Entity dyingEntity = event.getEntity();
        final Players players = gameContext.getPlayers();

        if (dyingEntity.getType() != EntityType.SHEEP || !dyingEntity.isCustomNameVisible()) {
            return;
        }

        // Make sure this was a player sheep that died and not anything else
        final String sheepName = dyingEntity.getCustomName();
        final GamePlayer ownerOfDyingSheep = gameContext.getPlayers().getPlayer(sheepName);
        if (ownerOfDyingSheep == null) {
            return;
        }

        playerDied(ownerOfDyingSheep);

//        gameContext.getTwitchChat().getTwitchClient().getChat().sendPrivateMessage(ownerOfDyingSheep.getName(), "Your sheep died.");

        final int numLivingSheep = players.getNumLivingSheep();

        // Only print the number of remaining sheep when it's "meaningful"
        final boolean shouldPrintMessage = numLivingSheep <= 10 || numLivingSheep % 5 == 0;

        if (shouldPrintMessage) {
            // This gets called after the entity is already dead, so this message will have the correct number.
            final List<GamePlayer> playersWithLivingSheep = gameContext.getPlayers().getPlayersWithLivingSheep();
            TextComponent remainingPlayerNames = Component.text("");
            if (playersWithLivingSheep.size() <= 10) {
                for (final GamePlayer player : playersWithLivingSheep) {
                    remainingPlayerNames = remainingPlayerNames.append(player.getColorfulName()).append(Component.text(" "));
                }

                remainingPlayerNames = Component.text(": ").append(remainingPlayerNames);
            }
            TextComponent txt1 = Component.text(numLivingSheep, TextColor.color(NamedTextColor.AQUA));
            TextComponent txt2 = Component.text(" sheep remaining");
            gameContext.getJavaPlugin().getServer().broadcast(MessageUtil.MergeTextComponents(txt1, txt2, remainingPlayerNames));
        }

        // We have a winner!
        if (numLivingSheep == 1) {
            final GamePlayer lastRemainingPlayer = players.getLastRemainingPlayer();
            final String titleText = String.format("%s wins!", lastRemainingPlayer.getName());
            final Arena arena = gameContext.getArena();
            final String subtitle = String.format("#1 of %d sheep", arena.getStartingNumSheep());
            arena.cancelSuddenDeath();
            gameContext.getFirstPlayer().sendTitle(titleText, subtitle, 10, 200, 20);
            gameContext.getTwitchChat().sendMessageToChannel(lastRemainingPlayer.getNameForTwitch() + " won the battle! PogChamp GG, everyone!");
            gameContext.scheduleArenaReset(300);
        }
    }

    @EventHandler
    public void onEntityTakeDamageFromOtherEntity(final EntityDamageByEntityEvent event) {
        final Entity damagingEntity = event.getDamager();
        final Entity damagedEntity = event.getEntity();

        // If it's not a sheep we aren't interested
        if (damagedEntity.getType() != EntityType.SHEEP) {
            return;
        }

        // Make sure the sheep was controlled by a player
        if (!damagedEntity.isCustomNameVisible()) return;

        // Make sure fireworks don't do damage since they're just intended to help players identify themselves
        if (damagingEntity.getType() == EntityType.FIREWORK_ROCKET) {
            event.setCancelled(true);
            return;
        }

        // We only care if they died
        if (event.getDamage() < ((LivingEntity) damagedEntity).getHealth()) {
            return;
        }

        final String sheepName = damagedEntity.getCustomName();
        final GamePlayer ownerOfDyingSheep = gameContext.getPlayers().getPlayer(sheepName);

        if (ownerOfDyingSheep == null) {
            return;
        }
        TextComponent deathMessage = null;
        String twitchDeathMessage = null;

        if (event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
            deathMessage = ownerOfDyingSheep.getColorfulName().append(Component.text(" was consumed by lava :(", TextColor.color(NamedTextColor.RED)));
            twitchDeathMessage = ownerOfDyingSheep.getNameForTwitch() + " was consumed by lava admFire";
        } else if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION && damagingEntity.getType() == EntityType.TNT) {
            final String tntName = damagingEntity.getCustomName();
            final GamePlayer ownerOfTnt = gameContext.getPlayers().getPlayer(tntName);
            if (ownerOfTnt == null) {
                return;
            }

            if (ownerOfTnt == ownerOfDyingSheep) {
                deathMessage = ownerOfDyingSheep.getColorfulName().append(Component.text(" blasted themselves :(", TextColor.color(NamedTextColor.RED)));
                twitchDeathMessage = ownerOfTnt.getNameForTwitch() + " blasted themselves admFire";
            } else {
                TextComponent txt1 = ownerOfTnt.getColorfulName();
                TextComponent txt2 = Component.text(" blasted ");
                TextComponent txt3 = ownerOfDyingSheep.getColorfulName();
                TextComponent txt4 = Component.text(" to smithereens", TextColor.color(NamedTextColor.RED));
                deathMessage = MessageUtil.MergeTextComponents(txt1, txt2, txt3, txt4);
                twitchDeathMessage = ownerOfTnt.getNameForTwitch() + " blasted " + ownerOfDyingSheep.getNameForTwitch() + " admNuke";

                incrementKillsForPlayer(ownerOfTnt);
            }
        }

        if (twitchDeathMessage != null) {
            gameContext.getTwitchChat().sendMessageToChannel(twitchDeathMessage);
        }
        if (deathMessage != null) {
            gameContext.getJavaPlugin().getServer().broadcast(deathMessage);
        }
    }

    private void playerDied(final GamePlayer gamePlayer) {
        Bukkit.getScheduler().runTask(gameContext.getJavaPlugin(), x -> {
            final Objective objective = gameContext.getKillsScoreboardObjective();
            final Score score = objective.getScore(gamePlayer.getNameColoredForInGameChat());
            final int origScore = score.getScore();
            Objects.requireNonNull(objective.getScoreboard()).resetScores(gamePlayer.getNameColoredForInGameChat());

            final Score newScore = objective.getScore(gamePlayer.getNameForScoreboardWhenDead());
            newScore.setScore(origScore);
        });
    }

    private void incrementKillsForPlayer(final GamePlayer gamePlayer) {
        Bukkit.getScheduler().runTask(gameContext.getJavaPlugin(), x -> {
            final Objective objective = gameContext.getKillsScoreboardObjective();
            Score score = objective.getScore(gamePlayer.getNameColoredForInGameChat());
            // It's possible that they died just before killing someone else, in which case we have to update their "dead" name on the leaderboard.
            if (!score.isScoreSet()) {
                score = objective.getScore(gamePlayer.getNameForScoreboardWhenDead());
            }
            score.setScore(score.getScore() + 1);
        });
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        gameContext.playerLeft(event.getPlayer());
    }

    @EventHandler
    public void onPlayerKick(final PlayerKickEvent event) {
        gameContext.playerLeft(event.getPlayer());
    }
}
