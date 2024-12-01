package live.adamlearns.animalroyale.extensions

import org.bukkit.scheduler.BukkitTask

fun BukkitTask.cancelIfNeeded() {
    if (!isCancelled) {
        cancel()
    }
}