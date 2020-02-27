package live.adamlearns.animalroyale;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import io.github.cdimascio.dotenv.Dotenv;

public class TwitchChat {

    private final TwitchClient twitchClient;

    public TwitchChat() {
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
        System.out.println("[" + event.getChannel().getName() + "] " + event.getUser().getName() + ": " + event.getMessage());
    }

    public void destroy() {
        this.twitchClient.getChat().disconnect();
    }
}
