package live.adamlearns.animalroyale.parser

object TntCommandParser {
    data class Parameters(
        val yaw: Int,
        val pitch: Int,
        val distance: Int,
        val ttl: Double,
    )

    private val YAW_WORDS: Map<String, Int> = mapOf(
        "top" to 0,
        "right" to 90,
        "bottom" to 180,
        "left" to 270,
        "t" to 0,
        "r" to 90,
        "b" to 180,
        "l" to 270,
        "north" to 0,
        "east" to 90,
        "south" to 180,
        "west" to 270,
        "n" to 0,
        "e" to 90,
        "s" to 180,
        "w" to 270,
        "northeast" to 45,
        "southeast" to 135,
        "southwest" to 225,
        "northwest" to 315,
        "ne" to 45,
        "se" to 135,
        "sw" to 225,
        "nw" to 315,
    )

    private const val DEFAULT_PITCH = 45
    private val PITCH_WORDS: Map<String, Int> = mapOf(
        "up" to 70,
        "down" to 20,
        "u" to 70,
        "d" to 20,
    )

    private const val DEFAULT_DISTANCE = 40
    private val DISTANCE_WORDS: Map<String, Int> = mapOf(
        "close" to 20,
        "far" to 60,
        "veryfar" to 80,
        "c" to 20,
        "f" to 60,
        "v" to 80,
    )

    private const val DEFAULT_TTL = 2.5
    private val TTL_WORDS: Map<String, Double> = mapOf(
        "quick" to 1.0,
        "slow" to 5.0,
        "q" to 1.0,
        "o" to 5.0,
    )

    fun parse(args: Array<String>): Parameters? = tryParsingRawParameters(args) ?: tryParsingSimpleParameters(args)

    private fun tryParsingRawParameters(args: Array<String>): Parameters? {
        if (args.size < 4) {
            return null
        }

        try {
            val yaw = args[0].toInt(10)

            // Minecraft considers -90 to be facing straight up, but most players will probably want to use positive numbers, so we invert this.
            val pitch = args[1].toInt(10) * -1
            val distance = args[2].toInt(10)
            val ttl = args[3].toDouble()

            return Parameters(yaw, pitch, distance, ttl)
        } catch (e: NumberFormatException) {
            return null
        }
    }

    private fun tryParsingSimpleParameters(args: Array<String>): Parameters? {
        var yawWord: String? = null
        var pitchWord: String? = null
        var distanceWord: String? = null
        var ttlWord: String? = null

        for (arg in args) {
            if (yawWord == null && YAW_WORDS.containsKey(arg)) {
                yawWord = arg
            }

            if (pitchWord == null && PITCH_WORDS.containsKey(arg)) {
                pitchWord = arg
            }

            if (distanceWord == null && DISTANCE_WORDS.containsKey(arg)) {
                distanceWord = arg
            }

            if (ttlWord == null && TTL_WORDS.containsKey(arg)) {
                ttlWord = arg
            }
        }

        // We need to have a yaw
        val yaw = YAW_WORDS[yawWord] ?: return null

        // Minecraft considers -90 to be facing straight up, but most players will probably want to use positive numbers, so we invert this.
        val pitch = -1 * (PITCH_WORDS[pitchWord] ?: DEFAULT_PITCH)
        val distance = DISTANCE_WORDS[distanceWord] ?: DEFAULT_DISTANCE
        val ttl = TTL_WORDS[ttlWord] ?: DEFAULT_TTL

        return Parameters(yaw, pitch, distance, ttl)
    }
}
