package live.adamlearns.animalroyale;

import org.bukkit.ChatColor;
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

            if (gameContext.getGamePhase() == GamePhase.LOBBY) {
                // Prevent the fall animation from happening since no damage is being taken
                event.setCancelled(true);
            }

            if (gameContext.getGamePhase() == GamePhase.GAMEPLAY) {
                final LivingEntity entity = (LivingEntity) event.getEntity();
                if (entity.getType() == EntityType.SHEEP && event.getDamage() >= entity.getHealth()) {
                    final String sheepName = entity.getCustomName();
                    final GamePlayer ownerOfSheep = gameContext.getPlayers().getPlayer(sheepName);
                    if (ownerOfSheep != null) {
                        final String deathMessage = ownerOfSheep.getNameColoredForInGameChat() + ChatColor.RED + " fell too far";
                        gameContext.getJavaPlugin().getServer().broadcastMessage(deathMessage);
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

        final int numLivingSheep = players.getNumLivingSheep();

        // Only print the number of remaining sheep when it's "meaningful"
        final boolean shouldPrintMessage = numLivingSheep <= 10 || numLivingSheep % 5 == 0;

        if (shouldPrintMessage) {
            // This gets called after the entity is already dead, so this message will have the correct number.
            gameContext.getJavaPlugin().getServer().broadcastMessage("" + ChatColor.AQUA + numLivingSheep + ChatColor.RESET + " sheep remaining");
        }

        // We have a winner!
        if (numLivingSheep == 1) {
            final GamePlayer lastRemainingPlayer = players.getLastRemainingPlayer();
            final String titleText = String.format("%s wins!", lastRemainingPlayer.getName());
            final String subtitle = String.format("#1 of %d sheep", gameContext.getArena().getStartingNumSheep());
            gameContext.getFirstPlayer().sendTitle(titleText, subtitle, 10, 200, 20);
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

        if (event.getDamage() < ((LivingEntity) damagedEntity).getHealth()) {
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION && damagingEntity.getType() == EntityType.PRIMED_TNT) {
            final String tntName = damagingEntity.getCustomName();
            final GamePlayer ownerOfTnt = gameContext.getPlayers().getPlayer(tntName);
            if (ownerOfTnt == null) {
                return;
            }

            final String sheepName = damagedEntity.getCustomName();
            final GamePlayer ownerOfDyingSheep = gameContext.getPlayers().getPlayer(sheepName);

            if (ownerOfDyingSheep == null) {
                return;
            }

            final String deathMessage;
            if (ownerOfTnt == ownerOfDyingSheep) {
                deathMessage = ownerOfTnt.getNameColoredForInGameChat() + ChatColor.RED + " blasted themselves :(";
            } else {
                deathMessage = ownerOfTnt.getNameColoredForInGameChat() + ChatColor.RED + " blasted " + ownerOfDyingSheep.getNameColoredForInGameChat() + ChatColor.RED + " to smithereens";
            }

            gameContext.getJavaPlugin().getServer().broadcastMessage(deathMessage);
        }
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
