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
