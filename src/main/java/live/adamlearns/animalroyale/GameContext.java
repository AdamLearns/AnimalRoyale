package live.adamlearns.animalroyale;

import org.bukkit.World;

/**
 * This keeps track of all classes needed in order for the plug-in to work.
 */
public class GameContext {
    private final GamePhase gamePhase;
    private World world;
    private TwitchChat twitchChat;

    public GameContext() {
        this.gamePhase = GamePhase.LOBBY;
    }

    public void registerWorld(final World world) {
        this.world = world;
    }

    public void registerTwitchChat(final TwitchChat twitchChat) {
        this.twitchChat = twitchChat;
    }
}
