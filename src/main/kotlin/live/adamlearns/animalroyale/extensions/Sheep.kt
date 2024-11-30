package live.adamlearns.animalroyale.extensions

import org.bukkit.entity.Sheep

val Sheep.isAliveAndValid: Boolean
    get() = this.isValid && !this.isDead
