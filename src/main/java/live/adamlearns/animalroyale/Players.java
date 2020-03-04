package live.adamlearns.animalroyale;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This keeps track of everyone who has typed a command via Twitch.
 */
public class Players {
    private final Map<String, GamePlayer> players = new HashMap<>();

    public void removeAllPlayers() {
        players.clear();
    }

    public GamePlayer getPlayer(final String name) {
        return players.get(name);
    }

    public Map<String, GamePlayer> getAllPlayers() {
        return players;
    }

    public int getNumLivingSheep() {
        return (int) players.values().stream().filter(GamePlayer::isSheepAlive).count();
    }

    public List<GamePlayer> getPlayersWithLivingSheep() {
        return players.values().stream().filter(GamePlayer::isSheepAlive).collect(Collectors.toList());
    }

    public GamePlayer getLastRemainingPlayer() {
        return players.values().stream().filter(GamePlayer::isSheepAlive).findFirst().orElse(null);
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
