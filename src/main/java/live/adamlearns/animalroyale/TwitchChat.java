package live.adamlearns.animalroyale;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import io.github.cdimascio.dotenv.Dotenv;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.entity.Sheep;

import java.util.Arrays;

public class TwitchChat {

    private final TwitchClient twitchClient;
    private final GameContext gameContext;

    public TwitchChat(final GameContext gameContext) {
        this.gameContext = gameContext;
        final Dotenv dotenv = Dotenv.configure().directory("./").load();
        final String oauthToken = dotenv.get("TWITCH_CHAT_OAUTH_TOKEN");
        final String twitchChannelToJoin = dotenv.get("TWITCH_CHANNEL_NAME");

        assert oauthToken != null;
        assert twitchChannelToJoin != null;

        final OAuth2Credential credential = new OAuth2Credential("twitch", oauthToken);

        this.twitchClient = TwitchClientBuilder.builder()
                .withEnableChat(true)
                .withChatAccount(credential)
                .build();
        twitchClient.getChat().joinChannel(twitchChannelToJoin);
        twitchClient.getEventManager().onEvent(ChannelMessageEvent.class).subscribe(this::onChatMessage);
    }

    private void onChatMessage(final ChannelMessageEvent event) {
        final String senderName = event.getUser().getName();

        final String message = event.getMessage().toLowerCase();
        final String[] commandAndArgs = message.split("\\s+");
        final String command = commandAndArgs[0];
        final String[] args = Arrays.copyOfRange(commandAndArgs, 1, commandAndArgs.length);

        if ((command.equals("!join") || command.equals("!color"))) {
            onJoin(senderName, args);
            return;
        }
    }

    private void onJoin(final String senderName, final String[] args) {
        if (args.length < 1 || !gameContext.canAddSheep()) {
            return;
        }

        final String color = args[0].toUpperCase();

        try {
            final DyeColor dyeColor = DyeColor.valueOf(color);

            final GamePlayer gamePlayer = gameContext.getPlayers().createPlayerIfNotExists(senderName);
            if (gamePlayer.canPlaceSheep()) {
                Bukkit.getScheduler().runTask(gameContext.getJavaPlugin(), x -> gameContext.getArena().createSheepForPlayer(gamePlayer, dyeColor));
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

    public void destroy() {
        this.twitchClient.getChat().disconnect();
    }
}
