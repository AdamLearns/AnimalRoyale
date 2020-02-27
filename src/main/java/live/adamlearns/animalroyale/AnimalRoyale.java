package live.adamlearns.animalroyale;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public final class AnimalRoyale extends JavaPlugin {

    private EventListener eventListener;
    private TwitchChat twitchChat;
    private final GameContext gameContext = new GameContext();

    private void setupWorld() {
        final World world = Bukkit.getServer().getWorld("world");
        assert world != null;

        // Turn off enemies so that they don't attack our sheep.
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);

        // Turn off weather so that we can always see the battles.
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);

        // Tiles won't drop items from them, which should speed things up.
        world.setGameRule(GameRule.DO_TILE_DROPS, false);

        gameContext.registerWorld(world);
    }

    private void setupEventListener() {
        this.eventListener = new EventListener();
        getServer().getPluginManager().registerEvents(eventListener, this);
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.setupWorld();
        this.setupEventListener();
        this.setupTwitchChat();

    }

    private void setupTwitchChat() {
        twitchChat = new TwitchChat();
        gameContext.registerTwitchChat(twitchChat);
    }

    @Override
    public void onDisable() {
        this.twitchChat.destroy();

        // Plugin shutdown logic
        if (this.eventListener != null) {
            HandlerList.unregisterAll(this.eventListener);
        }
    }
}
