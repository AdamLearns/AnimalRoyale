package live.adamlearns.animalroyale

import live.adamlearns.animalroyale.extensions.asTextColor
import live.adamlearns.animalroyale.extensions.clamp
import live.adamlearns.animalroyale.extensions.isAliveAndValid
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Sheep

/**
 * This is NOT the same as a Minecraft Player (i.e. an org.bukkit.entity.Player). This represents someone from Twitch
 * chat who has typed commands to play the game.
 */
class GamePlayer(
    /**
     * This is the person's Twitch name.
     */
    val name: String,
    private val displayName: String?
) {
    /**
     * This is as close to your sheep's color as we can get. We approximate some of them like black so that it's still
     * readable.
     */
    private var nameColor: TextColor? = null
    // This is essentially measured in System.currentTimeMillis
    private var nextTimeAbleToUseSpecialAbility: Long = 0

    var sheep: Sheep? = null
        set(value) {
            field = value
            nameColor = sheep?.color?.asTextColor()
        }

    val canPlaceSheep: Boolean
        get() = sheep == null

    val hasAddedSheep: Boolean
        get() = sheep != null

    val isSheepAlive: Boolean
        get() = sheep?.isAliveAndValid ?: false

    var hasSetTntParameters = false
        private set
    var tntNextYaw: Int = 0
        private set
    var tntNextPitch: Int = 0
        private set
    var tntNextPower: Int = 0
        private set
    var tntNextTtl: Double = 0.0
        private set

    val colorfulName: TextComponent
        get() = Component.text(displayName ?: name, nameColor)

    val nameForScoreboardWhenDead: TextComponent
        get() = Component.text(displayName ?: name).color(nameColor).decorate(TextDecoration.STRIKETHROUGH)

    val nameForTwitch: String
        get() = "@${displayName ?: name}"

    val cooldownMessage: String
        get() {
            val secRemaining =
                (nextTimeAbleToUseSpecialAbility - System.currentTimeMillis()) / 1000.0f
            return String.format("Your ability is on cooldown for another %.2fs", secRemaining)
        }

    fun setTntParameters(yaw: Int, pitch: Int, power: Int, ttl: Double) {
        setYaw(yaw)
        setPitch(pitch)
        setPower(power)
        setTtl(ttl)
        hasSetTntParameters = true
    }

    fun addYaw(yaw: Int) {
        setYaw(tntNextYaw + yaw)
    }

    fun addPitch(pitch: Int) {
        setPitch(tntNextPitch + pitch)
    }

    fun addPower(power: Int) {
        setPower(tntNextPower + power)
    }

    fun addTtl(ttl: Double) {
        setTtl(tntNextTtl + ttl)
    }

    fun canUseSpecialAbility(): Boolean {
        return System.currentTimeMillis() >= nextTimeAbleToUseSpecialAbility
    }

    fun setNextTimeAbleToUseSpecialAbility(nextTimeAbleToUseSpecialAbility: Long) {
        this.nextTimeAbleToUseSpecialAbility = nextTimeAbleToUseSpecialAbility
    }

    fun temporarilyStopShootingTnt() {
        hasSetTntParameters = false
    }

    private fun setYaw(yaw: Int) {
        tntNextYaw = yaw
    }

    private fun setPitch(pitch: Int) {
        tntNextPitch = pitch.clamp(MIN_PITCH, MAX_PITCH)
    }

    private fun setPower(power: Int) {
        tntNextPower = power.clamp(MIN_POWER, MAX_POWER)
    }

    private fun setTtl(ttl: Double) {
        tntNextTtl = ttl.clamp(MIN_TTL, MAX_TTL)
    }

    companion object {
        const val MIN_PITCH: Int = -90
        const val MAX_PITCH: Int = 90
        const val MIN_POWER: Int = 0
        const val MAX_POWER: Int = 100
        const val MIN_TTL: Double = 0.0
        const val MAX_TTL: Double = 5.0
    }
}
