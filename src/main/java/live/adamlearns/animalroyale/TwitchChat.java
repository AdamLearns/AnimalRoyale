package live.adamlearns.animalroyale;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.events.user.PrivateMessageEvent;
import io.github.cdimascio.dotenv.Dotenv;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class TwitchChat {

    private final TwitchClient twitchClient;
    private final GameContext gameContext;
    private final String twitchChannelToJoin;

    public TwitchChat(final GameContext gameContext) {
        this.gameContext = gameContext;
        final Dotenv dotenv = Dotenv.configure().directory("./").load();
        final String oauthToken = dotenv.get("TWITCH_CHAT_OAUTH_TOKEN");
        twitchChannelToJoin = dotenv.get("TWITCH_CHANNEL_NAME");

        assert oauthToken != null;
        assert twitchChannelToJoin != null;

        final OAuth2Credential credential = new OAuth2Credential("twitch", oauthToken);

        this.twitchClient = TwitchClientBuilder.builder()
                .withEnableChat(true)
                .withChatAccount(credential)
                .build();
        twitchClient.getChat().joinChannel(twitchChannelToJoin);
        twitchClient.getEventManager().onEvent(ChannelMessageEvent.class, this::onChatMessage);
        twitchClient.getEventManager().onEvent(PrivateMessageEvent.class, this::onWhisper);
    }

    public void sendMessageToChannel(final String text) {
        twitchClient.getChat().sendMessage(twitchChannelToJoin, text);
    }

    /**
     * Returns true if a Twitch user is considered to be an administrator of this plugin. For now, that's just the
     * channel owner.
     *
     * @param name
     * @return
     */
    private boolean isTwitchUserAnAdmin(final String name) {
        return name.equalsIgnoreCase(twitchChannelToJoin);
    }

    private void handleChatMessage(final String senderName, final String message) {
        final String[] commandAndArgs = message.split("\\s+");
        final String command = commandAndArgs[0];
        final String[] args = Arrays.copyOfRange(commandAndArgs, 1, commandAndArgs.length);

        if (command.equals("!startrounds") && isTwitchUserAnAdmin(senderName)) {
            gameContext.getArena().startRounds();
        }

        if (command.equals("!newarena") && isTwitchUserAnAdmin(senderName)) {
            gameContext.startNewGame();
        }

        if (command.equals("!lava") && isTwitchUserAnAdmin(senderName)) {
            gameContext.getArena().placeLavaRandomly();
        }

        if (command.equals("!identify")) {
            onIdentify(senderName, args);
            return;
        }

        if (command.equals("!where")) {
            onWhere(senderName, args);
            return;
        }

        if (command.equals("!teleport") || command.equals("!tp")) {
            onTeleport(senderName, args);
            return;
        }

        if (command.equals("!addyaw")) {
            onAddYaw(senderName, args);
            return;
        }
        if (command.equals("!addpitch")) {
            onAddPitch(senderName, args);
            return;
        }
        if (command.equals("!addpower")) {
            onAddPower(senderName, args);
            return;
        }
        if (command.equals("!addttl")) {
            onAddTtl(senderName, args);
            return;
        }

        if ((command.equals("!join") || command.equals("!color"))) {
            onJoin(senderName, args);
            return;
        }
        if (command.equals("!tnt")) {
            onTnt(senderName, args);
            return;
        }
        if (command.equals("!tntcancel") || command.equals("!tntstop")) {
            onTntCancel(senderName, args);
            return;
        }
    }

    private void onTntCancel(final String senderName, final String[] args) {
        final GamePlayer gamePlayer = gameContext.getPlayers().getPlayer(senderName);
        if (gamePlayer == null || !gamePlayer.isSheepAlive()) {
            return;
        }

        gamePlayer.temporarilyStopShootingTnt();
    }

    /**
     * We purposely don't restrict this by GamePhase; you can teleport while in the LOBBY phase.
     *
     * @param senderName
     * @param args
     */
    private void onTeleport(final String senderName, final String[] args) {
        final GamePlayer gamePlayer = gameContext.getPlayers().getPlayer(senderName);
        if (gamePlayer == null || !gamePlayer.isSheepAlive()) {
            return;
        }

        if (!gamePlayer.canUseSpecialAbility()) {
            System.err.println("Player " + senderName + " tried to teleport but their ability is on cooldown.");
//            gameContext.getTwitchChat().getTwitchClient().getChat().sendPrivateMessage(senderName, gamePlayer.getCooldownMessage());
            return;
        }

        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final int yaw;
        try {
            yaw = args.length > 0 ? Integer.parseInt(args[0], 10) : random.nextInt(359);
        } catch (final NumberFormatException e) {
            return;
        }
        final int distance = random.nextInt(8) + 3;

        final Sheep sheep = gamePlayer.getSheep();
        final double angleInRadians = yaw * Math.PI / 180.0;
        final Vector vector = new Vector(Math.sin(angleInRadians * -1), 0, Math.cos(angleInRadians)).normalize().multiply(distance);
        final Location sheepLocation = sheep.getLocation().clone();
        sheepLocation.add(vector);
        Util.setLocationToCenterOfBlock(sheepLocation);

        // Ensure that the new location is still inside the arena, otherwise they can teleport away from the battle
        if (!gameContext.getArena().isLocationInsideArena(sheepLocation)) {
            return;
        }

        sheepLocation.setY(gameContext.getWorld().getHighestBlockYAt(sheepLocation.getBlockX(), sheepLocation.getBlockZ()) + 2);

        gamePlayer.temporarilyStopShootingTnt();
        Bukkit.getScheduler().runTask(gameContext.getJavaPlugin(), x -> sheep.teleport(sheepLocation));

        gamePlayer.setNextTimeAbleToUseSpecialAbility(System.currentTimeMillis() + 15000);
    }

    private void onIdentify(final String senderName, final String[] args) {
        final GamePlayer gamePlayer = gameContext.getPlayers().getPlayer(senderName);
        if (gamePlayer == null || !gamePlayer.isSheepAlive()) {
            return;
        }

        Color color = Color.RED;
        String colorString = args.length > 0 ? args[0] : null;
        if (colorString != null) {
            colorString = colorString.toLowerCase();
            switch (colorString) {
                case "red":
                    color = Color.RED;
                    break;
                case "aqua":
                case "cyan":
                    color = Color.AQUA;
                    break;
                case "black":
                    color = Color.BLACK;
                    break;
                case "magenta":
                    color = Color.fromRGB(0xfc01ff);
                    break;
                case "grey":
                case "gray":
                    color = Color.GRAY;
                    break;
                case "lime":
                    color = Color.LIME;
                    break;
                case "pink":
                    color = Color.fromRGB(0xffaec9);
                    break;
                case "blue":
                    color = Color.BLUE;
                    break;
                case "orange":
                    color = Color.ORANGE;
                    break;
                case "green":
                    color = Color.GREEN;
                    break;
                case "yellow":
                    color = Color.YELLOW;
                    break;
                case "purple":
                    color = Color.PURPLE;
                    break;
                case "white":
                    color = Color.WHITE;
                    break;
            }
        }

        final Color fireworkColor = color;

        final Sheep sheep = gamePlayer.getSheep();

        Bukkit.getScheduler().runTask(gameContext.getJavaPlugin(), x -> {
            // There's no fall damage before the GAMEPLAY phase, so we can make the sheep jump
            if (gameContext.getGamePhase() == GamePhase.LOBBY) {
                sheep.setVelocity(new Vector(0, 2, 0));
            }

            for (int i = 0; i < 3; i++) {
                final FireworkEffect.Builder builder = FireworkEffect.builder();
                final FireworkEffect fireworkEffect = builder.trail(true).withColor(fireworkColor).build();
                final Firework firework = (Firework) gameContext.getWorld().spawnEntity(sheep.getLocation(), EntityType.FIREWORK_ROCKET);
                final FireworkMeta fireworkMeta = firework.getFireworkMeta();
                // Each power level is half a second of flight time
                fireworkMeta.setPower((i + 1) * 2);
                fireworkMeta.addEffect(fireworkEffect);
                firework.setFireworkMeta(fireworkMeta);
                firework.setVelocity(new Vector(0, (i + 1) * 0.2, 0));
            }
        });
    }

    private void onWhere(final String senderName, final String[] args) {
        final GamePlayer gamePlayer = gameContext.getPlayers().getPlayer(senderName);
        System.err.println(senderName + " used !where, but it doesn't do anything");
        if (gamePlayer == null) {
            return;
        }

        if (!gamePlayer.isSheepAlive()) {
//            twitchClient.getChat().sendPrivateMessage(senderName, "Your sheep already died. BibleThump");
            return;
        }

        final Sheep sheep = gamePlayer.getSheep();
        final Arena arena = gameContext.getArena();
        final Location sheepLocation = sheep.getLocation();
        final String relativeLocationInformationString = arena.getRelativeLocationInformationString(sheepLocation);

//        twitchClient.getChat().sendPrivateMessage(senderName, relativeLocationInformationString);
    }

    private void onWhisper(final PrivateMessageEvent event) {
        final String senderName = event.getUser().getName();
        final String message = event.getMessage().toLowerCase();

        handleChatMessage(senderName, message);
    }

    private void onChatMessage(final ChannelMessageEvent event) {
        final String senderName = event.getUser().getName();
        final String message = event.getMessage().toLowerCase();
        handleChatMessage(senderName, message);
    }

    private void onAddYaw(final String senderName, final String[] args) {
        if (args.length < 1) {
            return;
        }

        try {
            final int yaw = Integer.parseInt(args[0], 10);

            final GamePlayer gamePlayer = gameContext.getPlayers().getPlayer(senderName);
            if (gamePlayer == null) {
                return;
            }

            gamePlayer.addYaw(yaw);
            updateSheepRotationForPlayer(gamePlayer);
        } catch (final NumberFormatException e) {
            // Ignore formatting problems since people in Twitch chat will get this wrong quite a lot.
        }
    }

    private void onAddTtl(final String senderName, final String[] args) {
        if (args.length < 1) {
            return;
        }

        try {
            final double ttl = Double.parseDouble(args[0]);

            final GamePlayer gamePlayer = gameContext.getPlayers().getPlayer(senderName);
            if (gamePlayer == null) {
                return;
            }

            gamePlayer.addTtl(ttl);
        } catch (final NumberFormatException e) {
            // Ignore formatting problems since people in Twitch chat will get this wrong quite a lot.
        }
    }

    private void onAddPower(final String senderName, final String[] args) {
        if (args.length < 1) {
            return;
        }

        try {
            final int power = Integer.parseInt(args[0], 10);

            final GamePlayer gamePlayer = gameContext.getPlayers().getPlayer(senderName);
            if (gamePlayer == null) {
                return;
            }

            gamePlayer.addPower(power);
        } catch (final NumberFormatException e) {
            // Ignore formatting problems since people in Twitch chat will get this wrong quite a lot.
        }
    }

    private void onAddPitch(final String senderName, final String[] args) {
        if (args.length < 1) {
            return;
        }

        try {
            final int pitch = Integer.parseInt(args[0], 10) * -1;

            final GamePlayer gamePlayer = gameContext.getPlayers().getPlayer(senderName);
            if (gamePlayer == null) {
                return;
            }

            gamePlayer.addPitch(pitch);
            updateSheepRotationForPlayer(gamePlayer);
        } catch (final NumberFormatException e) {
            // Ignore formatting problems since people in Twitch chat will get this wrong quite a lot.
        }
    }

    private void updateSheepRotationForPlayer(final GamePlayer gamePlayer) {
        final int yaw = gamePlayer.getTntNextYaw();
        final int pitch = gamePlayer.getTntNextPitch();

        Bukkit.getScheduler().runTask(gameContext.getJavaPlugin(), x -> gamePlayer.getSheep().setRotation(yaw, pitch));
    }

    private void onTnt(final String senderName, final String[] args) {
        if (args.length < 4) {
            return;
        }

        try {
            final int yaw = Integer.parseInt(args[0], 10);

            // Minecraft considers -90 to be facing straight up, but most players will probably want to use positive numbers, so we invert this.
            final int pitch = Integer.parseInt(args[1], 10) * -1;
            final int distance = Integer.parseInt(args[2], 10);
            final double ttl = Double.parseDouble(args[3]);

            final GamePlayer gamePlayer = gameContext.getPlayers().getPlayer(senderName);
            if (gamePlayer == null) {
                return;
            }

            gamePlayer.setTntParameters(yaw, pitch, distance, ttl);
            updateSheepRotationForPlayer(gamePlayer);
        } catch (final NumberFormatException e) {
            // Ignore formatting problems since people in Twitch chat will get this wrong quite a lot.
        }
    }

    private void onJoin(final String senderName, final String[] args) {
        if (!gameContext.canAddSheep()) {
            return;
        }

        final String color = args.length > 0 ? args[0].toUpperCase() : Util.sampleArray(DyeColor.values()).name();

        try {
            final DyeColor dyeColor = DyeColor.valueOf(color);

            final GamePlayer gamePlayer = gameContext.getPlayers().createPlayerIfNotExists(senderName);
            if (gamePlayer.canPlaceSheep()) {
                Bukkit.getScheduler().runTask(gameContext.getJavaPlugin(), x -> {
                    final Arena arena = gameContext.getArena();
                    final Sheep sheep = arena.createSheepForPlayer(gamePlayer, dyeColor);
                    final String relativeLocationInformationString = arena.getRelativeLocationInformationString(sheep.getLocation());
//                    twitchClient.getChat().sendPrivateMessage(senderName, relativeLocationInformationString);
                });
            } else if (gamePlayer.hasAddedSheep()) {
                final Sheep sheep = gamePlayer.getSheep();

                Bukkit.getScheduler().runTask(gameContext.getJavaPlugin(), x -> sheep.setColor(dyeColor));
            }
        } catch (
                final IllegalArgumentException e) {
            // No point in spamming anything in this "catch" because Twitch chat loves to go bonkers, so they're going
            // to produce tons of exceptions.
        }
    }

    public TwitchClient getTwitchClient() {
        return twitchClient;
    }

    public void destroy() {
        this.twitchClient.getChat().disconnect();
    }
}
