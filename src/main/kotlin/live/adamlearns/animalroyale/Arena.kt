package live.adamlearns.animalroyale

import live.adamlearns.animalroyale.ComponentUtils.join
import live.adamlearns.animalroyale.exceptions.CannotLocateArenaException
import live.adamlearns.animalroyale.extensions.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.util.Ticks
import org.bukkit.*
import org.bukkit.block.Biome
import org.bukkit.entity.EntityType
import org.bukkit.entity.Sheep
import org.bukkit.entity.TNTPrimed
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.floor
import kotlin.math.max

class Arena(private val gameContext: GameContext) {
    /**
     * The arena is 2X * X, where X is this value. The 2X spans from east to west, and the X spans from north to south.
     */
    private val depth: Int = 40

    /**
     * This is in ticks.
     */
    private var timeUntilNextRound = 20 * Ticks.TICKS_PER_SECOND

    // This represents the top-center of the arena (since we're looking south).
    private var location: Location? = null

    /**
     * We periodically check if the arena is ready to have players join. When it is, this task will cancel itself.
     */
    private var checkArenaReadinessTask: BukkitTask? = null

    /**
     * This is the task that will automatically begin the current match, i.e. move from LOBBY to PRE_GAMEPLAY.
     */
    private var startCurrentMatchTask: BukkitTask? = null

    /**
     * This task is delayed so that we don't start a new match before the "end screen" from the last match is done.
     */
    private var startNewMatchTask: BukkitTask? = null

    /**
     * This has two purposes: scheduling sudden death originally, and then handling the periodic lava spawns.
     */
    private var suddenDeathTask: BukkitTask? = null

    var startingNumSheep: Int = 0

    var currentRoundStartTick: Float = Float.MIN_VALUE
        private set

    var nextRoundStartTick: Float = Float.MAX_VALUE
        private set

    private val length: Int
        get() = depth * 2

    private val isArenaReadyForGameplay: Boolean
        get() = haveNearbyChunksLoaded()

    init {
        setupNewArenaLocation()
    }

    /**
     * Finds a new location in the world for the Arena, sets the default camera position to it, clears out any existing
     * entities, etc.
     */
    private fun setupNewArenaLocation() {
        val world = gameContext.world
        try {
            location = getNextArenaLocation(world)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        setCameraPositionToStartingLocation()

        addWoolBorderToArena()

        setupArenaReadinessCheck()
    }

    fun cancelSuddenDeath() = suddenDeathTask?.cancelIfNeeded()

    fun dispose() {
        killAllSheep()

        startCurrentMatchTask?.cancelIfNeeded()
        suddenDeathTask?.cancelIfNeeded()
        startNewMatchTask?.cancelIfNeeded()
        checkArenaReadinessTask?.cancelIfNeeded()
    }

    /**
     * Checks every so often to see if the arena is ready, and when it is, this will advance the game state.
     */
    private fun setupArenaReadinessCheck() {
        checkArenaReadinessTask = Bukkit.getScheduler().runTaskTimer(gameContext.javaPlugin, Runnable {
            val tps = gameContext.javaPlugin.server.tps
            val lastMinuteTps = tps[0]

            // 20 TPS is the target, so we want to make sure things have stabilized enough
            if (lastMinuteTps < MINIMUM_TPS_TO_START) {
                return@Runnable
            }

            if (isArenaReadyForGameplay) {
                gameContext.advanceGamePhaseToLobby()
                scheduleStartOfMatch()
                checkArenaReadinessTask?.cancel()
            }
        }, 0, 2L * Ticks.TICKS_PER_SECOND)
    }

    private fun scheduleStartOfMatch() {
        gameContext.twitchChat?.sendMessageToChannel("Starting the round automatically in $NUM_SECONDS_BEFORE_STARTING_MATCH seconds.")
        startCurrentMatchTask = Bukkit.getScheduler().runTaskLater(
            gameContext.javaPlugin,
            this::startRounds,
            (NUM_SECONDS_BEFORE_STARTING_MATCH * Ticks.TICKS_PER_SECOND).toLong()
        )
    }

    private fun addWoolBorderToArena() {
        val world = checkNotNull(gameContext.world)
        val location = checkNotNull(this.location)

        val startX = location.westX(depth)
        val finalX = location.eastX(depth)
        // We are going to be facing south, which is the positive Z direction, so we only need to sample in that direction
        val startZ = location.northZ(depth)
        val finalZ = location.southZ(depth)

        Bukkit.getScheduler().runTask(gameContext.javaPlugin) { _ ->
            for (x in startX..finalX) {
                for (z in startZ..finalZ) {
                    if (x in (startX + 1) until finalX && z != startZ && z != finalZ) continue
                    if (z in (startZ + 1) until finalZ && x != startX && x != finalX) continue

                    val highestBlockYAt = world.getHighestBlockYAt(x, z)
                    val block = world.getBlockAt(x, highestBlockYAt + 1, z)
                    // Note: the block is never null, it's just air when you go high enough.
                    block.type = Material.YELLOW_WOOL

                    val block2 = world.getBlockAt(x, highestBlockYAt + 2, z)
                    block2.type = Material.LIGHT_BLUE_WOOL
                }
            }
        }
    }

    /**
     * Tests whether all nearby chunks have loaded that may eventually have sheep in them.
     *
     * @return
     */
    private fun haveNearbyChunksLoaded(): Boolean {
        val world = checkNotNull(gameContext.world)
        val location = checkNotNull(this.location)

        // This is how far away in any given direction from the center that we want to check
        val maxSampleDistance = depth

        // Each chunk is 16x16, so we only need to sample every 16 blocks
        val chunkSize = 16
        // This is how many blocks we travel in between samples.
        val centerX = location.blockX
        val centerZ = location.blockZ
        val startX = Math.floorDiv(centerX - maxSampleDistance, chunkSize)
        val finalX = Math.floorDiv(centerX + maxSampleDistance, chunkSize)
        // We are going to be facing south, which is the positive Z direction, so we only need to sample in that direction
        val startZ = Math.floorDiv(centerZ, chunkSize)
        val finalZ = Math.floorDiv(centerZ + maxSampleDistance, chunkSize)

        for (x in startX..finalX) {
            for (z in startZ..finalZ) {
                // Chunk coordinates are just block coordinates divided by 16. So loading chunk (0, 0) would load blocks (0-15, 0-15).
                if (!world.isChunkLoaded(x, z)) {
                    return false
                }
            }
        }

        return true
    }

    /**
     * This function exists as part of the cleanup for the arena. Without this, it's possible (though unlikely) that one
     * arena is close enough to another and a named sheep dies even after resetting everything. That could mess up the
     * stats and even the game state.
     */
    private fun killAllSheep() {
        Bukkit.getScheduler().runTask(gameContext.javaPlugin) { _ ->
            gameContext.players.allPlayers.values.filterNotNull()
                .filter { it.isSheepAlive }
                .forEach { it.sheep?.remove() }
        }
    }

    /**
     * After the arena's location has been settled, this will move the player so that they're looking at it from the
     * north.
     */
    private fun setCameraPositionToStartingLocation() {
        val world = checkNotNull(gameContext.world)
        val location = checkNotNull(this.location)

        val x = location.blockX
        val z = location.blockZ
        val highestBlock = world.getHighestBlockAt(x, z)
        val y = highestBlock.location.blockY + 42
        val yaw = 0f
        val pitch = 55f

        Bukkit.getScheduler().runTask(gameContext.javaPlugin) { _ ->
            gameContext.firstPlayer?.teleport(
                Location(
                    world,
                    x.toDouble(),
                    y.toDouble(),
                    z.toDouble(),
                    yaw,
                    pitch
                )
            )
        }
    }

    fun isLocationInsideArena(target: Location): Boolean {
        val location = checkNotNull(location)

        val blockX = target.blockX
        val blockZ = target.blockZ
        return blockX >= location.blockX - depth
                && blockX <= location.blockX + depth
                && blockZ >= location.blockZ
                && blockZ <= location.blockZ + depth
    }

    /**
     * Given a location, this will sample blocks every so often to make sure that none of them are in a bad biome. This
     * is because the arena is essentially a large rectangular portion of the world, so no part of that rectangle should
     * be in, say, a watery biome.
     *
     * @param location
     * @return
     */
    private fun isLocationValidForArena(location: Location): Boolean {
        val world = location.world

        // This location
        val coordinates = getArenaSampleCoordinates(location)

        for (vector in coordinates) {
            // Sampling blocks in unloaded parts of the world is not very performant since Minecraft has to load
            // each chunk. I think it's better to use world.getBiome.
            val biome = world.getBiome(vector.blockX, vector.blockY, vector.blockZ)
            if (BAD_BIOMES.contains(biome)) {
                return false
            }
        }

        return true
    }

    private fun getArenaSampleCoordinates(location: Location): Array<Vector> {
        val blockX = location.blockX
        val blockY = location.blockY
        val blockZ = location.blockZ

        // We'll just test the four corners of the arena and the center of it
        return arrayOf(
            Vector(blockX - depth, blockY, blockZ),
            Vector(blockX + depth, blockY, blockZ),
            Vector(blockX - depth, blockY, blockZ + depth),
            Vector(blockX + depth, blockY, blockZ + depth),
            Vector(blockX, blockY, blockZ + depth / 2),
        )
    }

    /**
     * Tries to find a good spot for the next arena. This picks a random portion in the world, ensures that it's "good",
     * and if not, retries the process.
     *
     * @param world
     * @return
     * @throws CannotLocateArenaException
     */
    @Throws(CannotLocateArenaException::class)
    private fun getNextArenaLocation(world: World?): Location {
        val random = ThreadLocalRandom.current()

        val maxNumAttempts = 1000
        var numAttempts = 0
        while (numAttempts++ < maxNumAttempts) {
            val randomLocation = Location(world, random.nextDouble() * 10000000, 255.0, random.nextDouble() * 10000000)
            if (isLocationValidForArena(randomLocation)) {
                return randomLocation
            }
        }

        throw CannotLocateArenaException("Could not find a valid location within $numAttempts attempts.")
    }

    /**
     * Creates a sheep for the given player. This is the player's avatar.
     *
     * @param gamePlayer
     * @param dyeColor
     * @return
     */
    internal fun createSheepForPlayer(gamePlayer: GamePlayer, dyeColor: DyeColor?): Sheep {
        val world = checkNotNull(gameContext.world)
        val sheepLocation = getNewLocationForSheep()
        val sheep = world.spawnEntity(sheepLocation, EntityType.SHEEP) as Sheep

        // We don't want sheep to have AI, but without AI, there's no fall damage, and we DO want fall damage. Instead,
        // we'll try adding a very slow potion.
        sheep.setAI(true)

        sheep.customName(Component.text(gamePlayer.name))
        sheep.isCustomNameVisible = true
        sheep.setRotation(90f, 45f)
        sheep.isAware = false
        sheep.isGlowing = true

        sheep.color = dyeColor

        gamePlayer.sheep = sheep

        // "location" represents the center of the Arena, so we need to subtract the sheep distance. Then, since we're
        // facing south, the coordinates go from high to low, so we invert them, that way people see it as it is on the screen.
        val relativeVector = getLocationRelativeToArena(sheepLocation)
        val sheepNumber = gameContext.players.numLivingSheep
        val txt1 = Component.text("Sheep #$sheepNumber:")
        val txt2 = gamePlayer.colorfulName
        val txt3 = Component.text("joined at")
        val txt4 = Component.text(
            "(${relativeVector.blockX}, ${relativeVector.blockZ}) ",
            TextColor.color(NamedTextColor.LIGHT_PURPLE)
        )
        gameContext.javaPlugin.server.broadcast(join(" ", txt1, txt2, txt3, txt4))

        return sheep
    }

    /**
     * Teleports a sheep to a new random location.
     */
    internal fun relocateSheepForPlayer(gamePlayer: GamePlayer) {
        val newSheepLocation = getNewLocationForSheep()

        Bukkit.getScheduler().runTask(gameContext.javaPlugin) { _ ->
            gamePlayer.sheep?.teleport(newSheepLocation)
        }
    }

    /**
     * Finds a suitable location for a sheep. If that location is in a liquid or a damaging block, it will also place a
     * safety block so the sheep stays above the hazard.
     */
    private fun getNewLocationForSheep(): Location {
        val world = checkNotNull(gameContext.world)
        val location = checkNotNull(this.location)
        val sheepLocation = location.clone()

        // Move to a random place within the arena
        val random = ThreadLocalRandom.current()
        sheepLocation.add(random.nextDouble() * depth * randomSign(), 0.0, random.nextDouble() * depth)

        val highestBlockAtSheepLocation = world.getHighestBlockAt(sheepLocation.blockX, sheepLocation.blockZ)
        val highestBlockType = highestBlockAtSheepLocation.type

        // Prevent the player from dying as soon as they spawn by placing a non-flammable block above the hazard.
        if (highestBlockAtSheepLocation.isLiquid || highestBlockType == Material.CACTUS || highestBlockType == Material.SWEET_BERRY_BUSH) {
            val blockAboveHighestBlock = world.getBlockAt(
                highestBlockAtSheepLocation.x,
                highestBlockAtSheepLocation.y + 1,
                highestBlockAtSheepLocation.z
            )
            blockAboveHighestBlock.type = Material.CYAN_CONCRETE
        }

        // Make it spawn a few blocks upwards so it looks like the sheep is falling
        sheepLocation.y = (world.getHighestBlockYAt(sheepLocation.blockX, sheepLocation.blockZ) + 30).toDouble()

        return sheepLocation
    }

    /**
     * Returns a vector whose coordinates will be in the range [0, dimension], where "dimension" is the length or depth
     * of the arena (based on which vector component you're looking at).
     *
     * @param target
     * @return
     */
    private fun getLocationRelativeToArena(target: Location): Vector {
        val location = checkNotNull(this.location)

        val sheepX = (depth * 2) - (target.blockX - (location.blockX - depth))
        val sheepZ = target.blockZ - location.blockZ

        return Vector(sheepX, target.blockY, sheepZ)
    }

    /**
     * Gets a string that says exactly where your sheep is located and then a human-readable form of that same string.
     *
     * @param sheepLocation
     * @return
     */
    private fun getRelativeLocationInformationString(sheepLocation: Location): String {
        val locationRelativeToArena = getLocationRelativeToArena(sheepLocation)
        val relativeLocationString = getHumanReadableRelativeLocation(sheepLocation)

        return String.format(
            "Your sheep is located at (%d, %d). The arena is %dx%d, which means you are in the %s.",
            locationRelativeToArena.blockX,
            locationRelativeToArena.blockZ,
            length,
            depth,
            relativeLocationString
        )
    }

    /**
     * Gets a string like "top-left corner" that represents which of the 9 sections of the arena you're in.
     *
     * @param location
     * @return
     */
    private fun getHumanReadableRelativeLocation(location: Location): String {
        val locationRelativeToArena = getLocationRelativeToArena(location)
        val x = locationRelativeToArena.blockX
        val z = locationRelativeToArena.blockZ

        // Split the whole arena into 9 equal portions and tell you which portion you fall under
        val divisionLength = length / 3
        val divisionDepth = depth / 3

        val locationString = if (x < divisionLength && z < divisionDepth) {
            "bottom-left corner"
        } else if (x < divisionLength && z < 2 * divisionDepth) {
            "middle of the left side"
        } else if (x < divisionLength) {
            "top-left corner"
        } else if (x < 2 * divisionLength && z < divisionDepth) {
            "middle of the bottom side"
        } else if (x < 2 * divisionLength && z < divisionDepth * 2) {
            "middle of the arena"
        } else if (x < 2 * divisionLength) {
            "middle of the top side"
        } else if (z < divisionDepth) {
            "bottom-right corner"
        } else if (z < divisionDepth * 2) {
            "middle of the right side"
        } else {
            "top-right corner"
        }

        return locationString
    }

    /**
     * This'll randomly spawn lava slightly above the arena, that way the match is greatly sped up.
     */
    private fun placeLavaRandomly() {
        val world = checkNotNull(gameContext.world)
        val location = checkNotNull(this.location)

        val random = ThreadLocalRandom.current()
        val lavaX = random.nextInt(location.westX(depth), location.eastX(depth) + 1)
        val lavaZ = random.nextInt(location.northZ(depth), location.southZ(depth) + 1)

        Bukkit.getScheduler().runTask(gameContext.javaPlugin) { _ ->
            val highestBlockY = world.getHighestBlockYAt(lavaX, lavaZ) + 5
            val block = world.getBlockAt(lavaX, highestBlockY, lavaZ)
            block.type = Material.LAVA
        }
    }

    /**
     * Starts a round where sheep will take their turns.
     */
    private fun startRound() {
        // Figure out everybody with TNT parameters and fire away
        gameContext.players.allPlayers.values.filterNotNull()
            .filter { it.hasSetTntParameters }
            .forEach { player ->
                val sheep = player.sheep?.takeIf { it.isAliveAndValid } ?: return@forEach

                Bukkit.getScheduler().runTask(gameContext.javaPlugin) { _ ->
                    createTntForSheep(sheep, player.tntNextPower, player.tntNextTtl)
                }
            }
    }

    /**
     * Fire TNT from all sheep.
     *
     * @param sheep
     * @param tntNextPower
     * @param tntNextTtl
     */
    private fun createTntForSheep(sheep: Sheep, tntNextPower: Int, tntNextTtl: Double) {
        val tnt = sheep.world.spawnEntity(sheep.location, EntityType.TNT) as TNTPrimed
        tnt.fuseTicks = (tntNextTtl * Ticks.TICKS_PER_SECOND).toInt()

        // We want to be able to identify this TNT later, so we store a name, but we don't want the name to be visible
        tnt.customName(sheep.customName())

        tnt.isCustomNameVisible = false

        val tntVector = sheep.location.direction
        tntVector.multiply(tntNextPower / 25.0)
        tnt.velocity = tntVector
    }

    fun startRounds() {
        startRoundIn(100)
        scheduleSuddenDeath()
        gameContext.advanceGamePhaseToPreGameplay()
    }

    private fun scheduleSuddenDeath() {
        val numTicksBeforeSuddenDeath = NUM_SECONDS_BEFORE_SUDDEN_DEATH * Ticks.TICKS_PER_SECOND

        suddenDeathTask = Bukkit.getScheduler().runTaskLater(
            gameContext.javaPlugin,
            this::startSuddenDeath,
            numTicksBeforeSuddenDeath.toLong()
        )
    }

    fun startSuddenDeath() {
        suddenDeathTask?.cancelIfNeeded()
        gameContext.twitchChat?.sendMessageToChannel("SUDDEN DEATH MODE ENGAGED! Lava will fall from the sky until all sheep are dead. ☠")

        // Repurpose the task into a periodic task that will spawn lava. By reusing the same variable, we will make
        // sure this gets canceled in the dispose function one way or another.
        suddenDeathTask = Bukkit.getScheduler().runTaskTimer(
            gameContext.javaPlugin,
            this::placeLavaRandomly,
            0,
            Ticks.TICKS_PER_SECOND / 2L
        )
    }

    private fun startRoundIn(delay: Long) {
        // Make sure we've advanced to GAMEPLAY
        if (gameContext.gamePhase == GamePhase.PRE_GAMEPLAY) {
            gameContext.advanceGamePhaseToGameplay()
        }

        startNewMatchTask = Bukkit.getScheduler().runTaskLater(gameContext.javaPlugin, Runnable {
            val currentTick = gameContext.javaPlugin.server.currentTick.toFloat()
            currentRoundStartTick = currentTick
            nextRoundStartTick = currentTick + timeUntilNextRound

            gameContext.arena?.startRound()
            startRoundIn(timeUntilNextRound.toLong())

            // The minimum possible turn time is 1 second
            timeUntilNextRound = max(Ticks.TICKS_PER_SECOND, floor(timeUntilNextRound * 0.9).toInt())
        }, delay)
    }

    companion object {
        const val MINIMUM_TPS_TO_START = 19.5
        const val NUM_SECONDS_BEFORE_STARTING_MATCH = 60
        const val NUM_SECONDS_BEFORE_SUDDEN_DEATH = 5 * 60

        /**
         * List of biomes considered a bad location for the arena, e.g. it has too much water or too many trees. Water isn't
         * necessarily a problem since we can cover liquids and hazards, but it's not a very easy arena to fight in. The one
         * ocean type that we DON'T consider bad just for some variety is Biome.FROZEN_OCEAN.
         */
        val BAD_BIOMES = arrayOf(
            Biome.OCEAN,
            Biome.JUNGLE,
            Biome.BIRCH_FOREST,
            Biome.BAMBOO_JUNGLE,
            Biome.COLD_OCEAN,
            Biome.DEEP_COLD_OCEAN,
            Biome.DEEP_LUKEWARM_OCEAN,
            Biome.DEEP_OCEAN,
            Biome.WARM_OCEAN,
            Biome.LUKEWARM_OCEAN
        )
    }
}
