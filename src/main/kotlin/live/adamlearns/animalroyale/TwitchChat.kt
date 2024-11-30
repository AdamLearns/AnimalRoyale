package live.adamlearns.animalroyale

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.twitch4j.TwitchClient
import com.github.twitch4j.TwitchClientBuilder
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import com.github.twitch4j.common.events.user.PrivateMessageEvent
import io.github.cdimascio.dotenv.Dotenv
import live.adamlearns.animalroyale.extensions.isAliveAndValid
import live.adamlearns.animalroyale.extensions.sample
import live.adamlearns.animalroyale.extensions.setToCenterOfBlock
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.DyeColor
import org.bukkit.FireworkEffect
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.util.Vector
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.cos
import kotlin.math.sin

class TwitchChat(private val gameContext: GameContext) {
    private val twitchClient: TwitchClient
    private val twitchChannelToJoin: String?

    init {
        val dotenv = Dotenv.configure().directory("./").load()
        val oauthToken = dotenv["TWITCH_CHAT_OAUTH_TOKEN"]
        twitchChannelToJoin = dotenv["TWITCH_CHANNEL_NAME"]

        checkNotNull(oauthToken)
        checkNotNull(twitchChannelToJoin)

        val credential = OAuth2Credential("twitch", oauthToken)

        this.twitchClient = TwitchClientBuilder.builder()
            .withEnableChat(true)
            .withChatAccount(credential)
            .build()
        twitchClient.chat.joinChannel(twitchChannelToJoin)
        twitchClient.eventManager.onEvent(ChannelMessageEvent::class.java, this::onChatMessage)
        twitchClient.eventManager.onEvent(PrivateMessageEvent::class.java, this::onWhisper)
    }

    fun sendMessageToChannel(text: String?) {
        twitchClient.chat.sendMessage(twitchChannelToJoin, text)
    }

    /**
     * Returns true if a Twitch user is considered to be an administrator of this plugin. For now, that's just the
     * channel owner.
     *
     * @param name
     * @return
     */
    private fun isTwitchUserAnAdmin(name: String): Boolean {
        return name.equals(twitchChannelToJoin, ignoreCase = true)
    }

    private fun handleChatMessage(senderName: String, message: String) {
        val commandAndArgs = message.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val command = commandAndArgs[0]
        val args = Arrays.copyOfRange(commandAndArgs, 1, commandAndArgs.size)

        if (command == "!startrounds" && isTwitchUserAnAdmin(senderName)) {
            gameContext.arena?.startRounds()
        }

        if (command == "!newarena" && isTwitchUserAnAdmin(senderName)) {
            gameContext.startNewGame()
        }

        if (command == "!lava" && isTwitchUserAnAdmin(senderName)) {
            gameContext.arena?.startSuddenDeath()
        }

        when (command) {
            "!identify" -> {
                onIdentify(senderName, args)
                return
            }

            "!teleport", "!tp" -> {
                onTeleport(senderName, args)
                return
            }

            "!addyaw" -> {
                onAddYaw(senderName, args)
                return
            }

            "!addpitch" -> {
                onAddPitch(senderName, args)
                return
            }

            "!addpower" -> {
                onAddPower(senderName, args)
                return
            }

            "!addttl" -> {
                onAddTtl(senderName, args)
                return
            }

            "!join", "!color" -> {
                onJoin(senderName, args)
                return
            }

            "!tnt" -> {
                onTnt(senderName, args)
                return
            }

            "!tntcancel", "!tntstop" -> {
                onTntCancel(senderName, args)
                return
            }
        }
    }

    private fun onTntCancel(senderName: String, args: Array<String>) {
        val gamePlayer = gameContext.players.getPlayer(senderName)?.takeIf { it.isSheepAlive } ?: return
        gamePlayer.temporarilyStopShootingTnt()
    }

    /**
     * We purposely don't restrict this by GamePhase; you can teleport while in the LOBBY phase.
     *
     * @param senderName
     * @param args
     */
    private fun onTeleport(senderName: String, args: Array<String>) {
        val world = gameContext.world ?: return
        val arena = gameContext.arena ?: return
        val gamePlayer = gameContext.players.getPlayer(senderName) ?: return
        val sheep = gamePlayer.sheep?.takeIf { it.isAliveAndValid } ?: return

        if (!gamePlayer.canUseSpecialAbility()) {
            System.err.println("Player $senderName tried to teleport but their ability is on cooldown.")
//            gameContext.getTwitchChat().getTwitchClient().getChat().sendPrivateMessage(senderName, gamePlayer.getCooldownMessage());
            return
        }

        val random = ThreadLocalRandom.current()

        val yaw: Int
        try {
            yaw = if (args.isNotEmpty()) args[0].toInt(10) else random.nextInt(359)
        } catch (e: NumberFormatException) {
            return
        }
        val distance = random.nextInt(8) + 3

        val angleInRadians = yaw * Math.PI / 180.0
        val vector = Vector(sin(angleInRadians * -1), 0.0, cos(angleInRadians)).normalize().multiply(distance)
        val sheepLocation = sheep.location.clone()
        sheepLocation.add(vector)
        sheepLocation.setToCenterOfBlock()

        // Ensure that the new location is still inside the arena, otherwise they can teleport away from the battle
        if (!arena.isLocationInsideArena(sheepLocation)) {
            return
        }

        sheepLocation.y =
            (world.getHighestBlockYAt(sheepLocation.blockX, sheepLocation.blockZ) + 2).toDouble()

        gamePlayer.temporarilyStopShootingTnt()
        Bukkit.getScheduler().runTask(gameContext.javaPlugin) { _ -> sheep.teleport(sheepLocation) }

        gamePlayer.setNextTimeAbleToUseSpecialAbility(System.currentTimeMillis() + 15000)
    }

    private fun onIdentify(senderName: String, args: Array<String>) {
        val gamePlayer = gameContext.players.getPlayer(senderName) ?: return
        val sheep = gamePlayer.sheep?.takeIf { it.isAliveAndValid } ?: return

        val fireworkColor: Color = if (args.isNotEmpty()) {
            when (args[0].lowercase(Locale.getDefault())) {
                "aqua", "cyan" -> Color.AQUA
                "black" -> Color.BLACK
                "magenta" -> Color.fromRGB(0xfc01ff)
                "grey", "gray" -> Color.GRAY
                "lime" -> Color.LIME
                "pink" -> Color.fromRGB(0xffaec9)
                "blue" -> Color.BLUE
                "orange" -> Color.ORANGE
                "green" -> Color.GREEN
                "yellow" -> Color.YELLOW
                "purple" -> Color.PURPLE
                "white" -> Color.WHITE
                "red" -> Color.RED
                else -> Color.RED
            }
        } else {
            Color.RED
        }

        Bukkit.getScheduler().runTask(gameContext.javaPlugin) { _ ->
            // There's no fall damage before the GAMEPLAY phase, so we can make the sheep jump
            if (gameContext.gamePhase == GamePhase.LOBBY) {
                sheep.velocity = Vector(0, 2, 0)
            }

            for (i in 0..2) {
                val firework =
                    (gameContext.world?.spawnEntity(sheep.location, EntityType.FIREWORK_ROCKET) as Firework?) ?: continue
                val fireworkMeta = firework.fireworkMeta

                val fireworkEffect = FireworkEffect.builder()
                    .trail(true)
                    .withColor(fireworkColor)
                    .build()

                // Each power level is half a second of flight time
                fireworkMeta.power = (i + 1) * 2
                fireworkMeta.addEffect(fireworkEffect)
                firework.fireworkMeta = fireworkMeta
                firework.velocity = Vector(0.0, (i + 1) * 0.2, 0.0)
            }
        }
    }

    private fun onWhisper(event: PrivateMessageEvent) {
        val senderName = event.user.name
        val message = event.message.lowercase(Locale.getDefault())

        handleChatMessage(senderName, message)
    }

    private fun onChatMessage(event: ChannelMessageEvent) {
        val senderName = event.user.name
        val message = event.message.lowercase(Locale.getDefault())
        handleChatMessage(senderName, message)
    }

    private fun onAddYaw(senderName: String, args: Array<String>) {
        if (args.isEmpty()) {
            return
        }

        try {
            val yaw = args[0].toInt(10)

            val gamePlayer = gameContext.players.getPlayer(senderName) ?: return

            gamePlayer.addYaw(yaw)
            updateSheepRotationForPlayer(gamePlayer)
        } catch (e: NumberFormatException) {
            // Ignore formatting problems since people in Twitch chat will get this wrong quite a lot.
        }
    }

    private fun onAddTtl(senderName: String, args: Array<String>) {
        if (args.isEmpty()) {
            return
        }

        try {
            val ttl = args[0].toDouble()

            val gamePlayer = gameContext.players.getPlayer(senderName) ?: return

            gamePlayer.addTtl(ttl)
        } catch (e: NumberFormatException) {
            // Ignore formatting problems since people in Twitch chat will get this wrong quite a lot.
        }
    }

    private fun onAddPower(senderName: String, args: Array<String>) {
        if (args.isEmpty()) {
            return
        }

        try {
            val power = args[0].toInt(10)

            val gamePlayer = gameContext.players.getPlayer(senderName) ?: return

            gamePlayer.addPower(power)
        } catch (e: NumberFormatException) {
            // Ignore formatting problems since people in Twitch chat will get this wrong quite a lot.
        }
    }

    private fun onAddPitch(senderName: String, args: Array<String>) {
        if (args.isEmpty()) {
            return
        }

        try {
            val pitch = args[0].toInt(10) * -1

            val gamePlayer = gameContext.players.getPlayer(senderName) ?: return

            gamePlayer.addPitch(pitch)
            updateSheepRotationForPlayer(gamePlayer)
        } catch (e: NumberFormatException) {
            // Ignore formatting problems since people in Twitch chat will get this wrong quite a lot.
        }
    }

    private fun updateSheepRotationForPlayer(gamePlayer: GamePlayer) {
        val yaw = gamePlayer.tntNextYaw
        val pitch = gamePlayer.tntNextPitch

        Bukkit.getScheduler().runTask(gameContext.javaPlugin) { _ ->
            gamePlayer.sheep?.setRotation(yaw.toFloat(), pitch.toFloat())
        }
    }

    private fun onTnt(senderName: String, args: Array<String>) {
        if (args.size < 4) {
            return
        }

        try {
            val yaw = args[0].toInt(10)

            // Minecraft considers -90 to be facing straight up, but most players will probably want to use positive numbers, so we invert this.
            val pitch = args[1].toInt(10) * -1
            val distance = args[2].toInt(10)
            val ttl = args[3].toDouble()

            val gamePlayer = gameContext.players.getPlayer(senderName) ?: return

            gamePlayer.setTntParameters(yaw, pitch, distance, ttl)
            updateSheepRotationForPlayer(gamePlayer)
        } catch (e: NumberFormatException) {
            // Ignore formatting problems since people in Twitch chat will get this wrong quite a lot.
        }
    }

    private fun onJoin(senderName: String, args: Array<String>) {
        if (!gameContext.canAddSheep()) {
            return
        }

        val arena = gameContext.arena ?: return
        val gamePlayer = gameContext.players.createPlayerIfNotExists(senderName) ?: return

        val colorName =
            if (args.isNotEmpty()) args[0].uppercase(Locale.getDefault()) else DyeColor.values().sample().name

        try {
            val dyeColor = DyeColor.valueOf(colorName)

            if (gamePlayer.canPlaceSheep) {
                Bukkit.getScheduler().runTask(gameContext.javaPlugin) { _ ->
                    /*val sheep =*/ arena.createSheepForPlayer(gamePlayer, dyeColor)
//                    val relativeLocationInformationString = arena.getRelativeLocationInformationString(sheep.location)
//                    twitchClient.chat.sendPrivateMessage(senderName, relativeLocationInformationString)
                }
            } else if (gamePlayer.hasAddedSheep) {
                Bukkit.getScheduler().runTask(gameContext.javaPlugin) { _ -> gamePlayer.sheep?.color = dyeColor }
            }
        } catch (e: IllegalArgumentException) {
            // No point in spamming anything in this "catch" because Twitch chat loves to go bonkers, so they're going
            // to produce tons of exceptions.
        }
    }

    fun destroy() {
        twitchClient.chat.disconnect()
    }
}
