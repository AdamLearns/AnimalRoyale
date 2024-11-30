package live.adamlearns.animalroyale

/**
 * This keeps track of everyone who has typed a command via Twitch.
 */
class Players {
    private val players: MutableMap<String, GamePlayer?> = HashMap()

    val allPlayers: Map<String, GamePlayer?>
        get() = players

    val numLivingSheep: Int
        get() = players.values.count { it?.isSheepAlive ?: false }

    val playersWithLivingSheep: List<GamePlayer?>
        get() = players.values.filter { it?.isSheepAlive ?: false }

    val lastRemainingPlayer: GamePlayer?
        get() = players.values.firstOrNull { it?.isSheepAlive ?: false }

    fun removeAllPlayers() {
        players.clear()
    }

    fun getPlayer(name: String): GamePlayer? {
        return players[name]
    }

    fun createPlayerIfNotExists(name: String): GamePlayer? {
        if (players.containsKey(name)) {
            return players[name]
        }

        val gamePlayer = GamePlayer(name)
        players[name] = gamePlayer

        return gamePlayer
    }
}
