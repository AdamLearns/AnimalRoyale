package live.adamlearns.animalroyale

import org.bukkit.Bukkit
import org.bukkit.GameRule
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin

class AnimalRoyale : JavaPlugin() {
    private var eventListener: EventListener? = null
    private var twitchChat: TwitchChat? = null
    private val gameContext = GameContext(this)

    private fun setupWorld() {
        val world = checkNotNull(Bukkit.getServer().getWorld("world"))
        // Turn off enemies so that they don't attack our sheep.
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false)

        // Turn off weather so that we can always see the battles.
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false)

        // Don't drop items since no one collects them
        world.setGameRule(GameRule.DO_TILE_DROPS, false)
        world.setGameRule(GameRule.DO_MOB_LOOT, false)

        gameContext.registerWorld(world)
    }

    private fun setupEventListener() {
        this.eventListener = EventListener(gameContext)
        server.pluginManager.registerEvents(eventListener!!, this)
    }

    override fun onEnable() {
        // Plugin startup logic
        this.setupWorld()
        this.setupEventListener()
        this.setupTwitchChat()
    }

    private fun setupTwitchChat() {
        twitchChat = TwitchChat(gameContext)
        gameContext.registerTwitchChat(twitchChat)
    }

    override fun onDisable() {
        twitchChat?.destroy()

        // Plugin shutdown logic
        if (this.eventListener != null) {
            HandlerList.unregisterAll(eventListener!!)
        }
    }
}
