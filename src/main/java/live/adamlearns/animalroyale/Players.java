package live.adamlearns.animalroyale;

import java.util.HashMap;
import java.util.Map;

/**
 * This keeps track of everyone who has typed a command via Twitch.
 */
public class Players {
    private final Map<String, GamePlayer> players;

    public Players() {
        players = new HashMap<>();
    }

    public GamePlayer createPlayerIfNotExists(final String name) {
        if (players.containsKey(name)) {
            return players.get(name);
        }

        final GamePlayer gamePlayer = new GamePlayer(name);
        players.put(name, gamePlayer);

        return gamePlayer;
    }
}
