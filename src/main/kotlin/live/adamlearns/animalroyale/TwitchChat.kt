package live.adamlearns.animalroyale

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.twitch4j.TwitchClient
import com.github.twitch4j.TwitchClientBuilder
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import com.github.twitch4j.common.enums.CommandPermission
import io.github.cdimascio.dotenv.Dotenv
import live.adamlearns.animalroyale.extensions.distanceTo
import live.adamlearns.animalroyale.extensions.isAliveAndValid
import live.adamlearns.animalroyale.extensions.setToCenterOfBlock
import live.adamlearns.animalroyale.parser.TntCommandParser
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.DyeColor
import org.bukkit.FireworkEffect
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.util.Vector
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import java.util.regex.Pattern
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

    private fun onChatMessage(event: ChannelMessageEvent) {
        val senderName = event.user.name
        val senderDisplayName = event.messageEvent.userDisplayName.orElse(null)
        val senderChatColor: String? = event.messageEvent.userChatColor.orElse(null)
        val senderIsAdmin = isTwitchUserAnAdmin(senderName) || event.permissions.any {
            ADMIN_PERMISSIONS.contains(it)
        }

        val message = event.message.lowercase(Locale.getDefault())
        val commandAndArgs = message.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val command = commandAndArgs[0]
        val args = Arrays.copyOfRange(commandAndArgs, 1, commandAndArgs.size)

        if (command == "!startrounds" && senderIsAdmin) {
            gameContext.arena?.startRounds()
        }

        if (command == "!newarena" && senderIsAdmin) {
            gameContext.startNewGame()
        }

        if (command == "!lava" && senderIsAdmin) {
            gameContext.arena?.startSuddenDeath()
        }

        when (command) {
            "!identify" -> onIdentify(senderName, args)
            "!teleport", "!tp" -> onTeleport(senderName, args)
            "!addyaw" -> onAddYaw(senderName, args)
            "!addpitch" -> onAddPitch(senderName, args)
            "!addpower" -> onAddPower(senderName, args)
            "!addttl" -> onAddTtl(senderName, args)
            "!join", "!color" -> onJoin(senderName, senderDisplayName, senderChatColor, args)
            "!relocate" -> onRelocate(senderName, args)
            "!tnt" -> onTnt(senderName, args)
            "!tntcancel", "!tntstop" -> onTntCancel(senderName, args)
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

    private fun onRelocate(senderName: String, args: Array<String>) {
        // This is only allowed in the 'lobby' (i.e. when players join and before the round starts)
        if (gameContext.gamePhase != GamePhase.LOBBY) {
            return
        }

        val arena = gameContext.arena ?: return
        val gamePlayer = gameContext.players.getPlayer(senderName) ?: return

        arena.relocateSheepForPlayer(gamePlayer)
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
        val gamePlayer = gameContext.players.getPlayer(senderName) ?: return
        val parameters = TntCommandParser.parse(args) ?: return
        gamePlayer.setTntParameters(parameters.yaw, parameters.pitch, parameters.distance, parameters.ttl)
        updateSheepRotationForPlayer(gamePlayer)
    }

    private fun onJoin(senderName: String, senderDisplayName: String?, senderChatColor: String?, args: Array<String>) {
        if (!gameContext.canAddSheep()) {
            return
        }

        val arena = gameContext.arena ?: return
        val gamePlayer = gameContext.players.createPlayerIfNotExists(senderName, senderDisplayName) ?: return

        val colorName = if (args.isNotEmpty()) args[0].uppercase(Locale.getDefault()) else null

        try {
            val dyeColor = colorName?.let { DyeColor.valueOf(it) }
                ?: senderChatColor?.trim()
                    ?.takeIf { isValidHexString(it) }
                    ?.let { getDyeColorFromHex(senderChatColor) }
                ?: DyeColor.values().random()

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

    companion object {
        val ADMIN_PERMISSIONS = listOf(CommandPermission.OWNER, CommandPermission.BROADCASTER, CommandPermission.MODERATOR)

        private val HEX_PATTERN = Pattern.compile("^#[A-Fa-f0-9]{6}$")
        private var cachedClosestColors: MutableMap<String, DyeColor> = mutableMapOf()

        private fun isValidHexString(hex: String): Boolean {
            return HEX_PATTERN.matcher(hex).matches()
        }

        private fun getDyeColorFromHex(hex: String): DyeColor =
            // These are the 'default' Twitch colors. For custom colors, we try to find the closest one.
            when (hex) {
                // rgb(255, 0, 0)
                "#FF0000" -> DyeColor.RED
                // rgb(178, 34, 34)
                "#B22222" -> DyeColor.RED
                // rgb(0, 128, 0)
                "#008000" -> DyeColor.GREEN
                // rgb(46, 139, 87)
                "#2E8B57" -> DyeColor.CYAN
                // rgb(154, 205, 50)
                "#91CD32" -> DyeColor.LIME
                // rgb(0, 255, 127)
                "#00FF7F" -> DyeColor.LIME
                // rgb(0, 0, 255)
                "#0000FF" -> DyeColor.BLUE
                // rgb(30, 144, 255)
                "#1E90FF" -> DyeColor.BLUE
                // rgb(95, 158, 160)
                "#5F9EA0" -> DyeColor.LIGHT_BLUE
                // rgb(255, 127, 80)
                "#FF7F50" -> DyeColor.ORANGE
                // rgb(255, 69, 0)
                "#FF4500" -> DyeColor.ORANGE
                // rgb(210, 105, 30)
                "#D2691E" -> DyeColor.ORANGE
                // rgb(218, 165, 32)
                "#DAA520" -> DyeColor.YELLOW
                // rgb(255, 105, 180)
                "#FF69B4" -> DyeColor.PINK
                // rgb(138, 43, 226)
                "#8A2BE2" -> DyeColor.PURPLE
                else -> getClosestDyeColorFromHex(hex)
            }

        private fun getClosestDyeColorFromHex(hex: String): DyeColor {
            if (cachedClosestColors.containsKey(hex)) {
                return cachedClosestColors[hex]!!
            }

            val color = Color.fromRGB(hex.removePrefix("#").toInt(16))

            var closestDyeColor = DyeColor.WHITE
            var closesDistance = Double.MAX_VALUE

            for (dyeColor in DyeColor.values()) {
                val distance = color.distanceTo(dyeColor.color)

                if (distance < closesDistance) {
                    closestDyeColor = dyeColor
                    closesDistance = distance
                }
            }

            cachedClosestColors[hex] = closestDyeColor
            return closestDyeColor
        }
    }
}
