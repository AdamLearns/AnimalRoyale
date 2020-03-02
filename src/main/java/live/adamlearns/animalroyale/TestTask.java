package live.adamlearns.animalroyale;

import org.bukkit.DyeColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class TestTask extends BukkitRunnable {
    private final JavaPlugin plugin;
    private final String senderName;
    private final DyeColor dyeColor;
    private final Arena arena;

    public TestTask(final JavaPlugin plugin, final String senderName, final DyeColor dyeColor, final Arena arena) {
        this.plugin = plugin;
        this.senderName = senderName;
        this.dyeColor = dyeColor;
        this.arena = arena;
    }

    @Override
    public void run() {
        // What you want to schedule goes here
        arena.createSheepForPlayer(senderName, dyeColor);
    }
}
