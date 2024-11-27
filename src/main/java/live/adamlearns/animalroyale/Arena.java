package live.adamlearns.animalroyale;

import net.kyori.adventure.util.Ticks;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

public class Arena {
    private final GameContext gameContext;

    /**
     * The arena is 2X * X, where X is this value. The 2X spans from east to west, and the X spans from north to south.
     */
    private final int arenaSize = 40;

    /**
     * This is in ticks.
     */
    private int timeBetweenTurns = 20 * Ticks.TICKS_PER_SECOND;

    // This represents the top-center of the arena (since we're looking south).
    private Location location;

    /**
     * We periodically check if the arena is ready to have players join. When it is, this task will cancel itself.
     */
    private BukkitTask checkArenaReadinessTask = null;
    private int startingNumSheep;

    /**
     * This is the task that will automatically begin the current match, i.e. move from LOBBY to PRE_GAMEPLAY.
     */
    private BukkitTask startCurrentMatchTask;

    /**
     * This task is delayed so that we don't start a new match before the "end screen" from the last match is done.
     */
    private BukkitTask startNewMatchTask;

    /**
     * This has two purposes: scheduling sudden death originally, and then handling the periodic lava spawns.
     */
    private BukkitTask suddenDeathTask;

    public Arena(final GameContext gameContext) {
        this.gameContext = gameContext;

        setupNewArenaLocation();
    }

    /**
     * Finds a new location in the world for the Arena, sets the default camera position to it, clears out any existing
     * entities, etc.
     */
    private void setupNewArenaLocation() {
        final World world = gameContext.getWorld();
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        try {
            location = getNextArenaLocation(world);
        } catch (final Exception e) {
            e.printStackTrace();
            throw e;
        }

        setCameraPositionToStartingLocation();

        addWoolBorderToArena();

        setupArenaReadinessCheck();
    }

    /**
     * Gets the X coordinate of the western boundary of the arena.
     *
     * @return
     */
    private int getWestX() {
        final int centerX = location.getBlockX();
        return centerX - arenaSize;
    }

    /**
     * Gets the X coordinate of the eastern boundary of the arena.
     *
     * @return
     */
    private int getEastX() {
        final int centerX = location.getBlockX();
        return centerX + arenaSize;
    }

    private int getCenterX() {
        return (getEastX() + getWestX()) / 2;
    }

    private int getCenterZ() {
        return (getNorthZ() + getSouthZ()) / 2;
    }

    /**
     * Gets the X coordinate of the northern boundary of the arena.
     *
     * @return
     */
    private int getNorthZ() {
        return location.getBlockZ();
    }

    /**
     * Gets the X coordinate of the southern boundary of the arena.
     *
     * @return
     */
    private int getSouthZ() {
        return location.getBlockZ() + arenaSize;
    }

    public void dispose() {
        this.killAllSheep();

        if (startCurrentMatchTask != null && !startCurrentMatchTask.isCancelled()) {
            startCurrentMatchTask.cancel();
        }
        cancelSuddenDeath();
        if (startNewMatchTask != null && !startNewMatchTask.isCancelled()) {
            startNewMatchTask.cancel();
        }
        if (checkArenaReadinessTask != null && !checkArenaReadinessTask.isCancelled()) {
            checkArenaReadinessTask.cancel();
        }
    }

    public void cancelSuddenDeath() {
        if (suddenDeathTask != null && !suddenDeathTask.isCancelled()) {
            suddenDeathTask.cancel();
        }
    }

    /**
     * Checks every so often to see if the arena is ready, and when it is, this will advance the game state.
     */
    private void setupArenaReadinessCheck() {
        checkArenaReadinessTask = Bukkit.getScheduler().runTaskTimer(gameContext.getJavaPlugin(), () -> {
            final double[] tps = gameContext.getJavaPlugin().getServer().getTPS();
            final double lastMinuteTps = tps[0];

            // 20 TPS is the target, so we want to make sure things have stabilized enough
            if (lastMinuteTps < 19.5) {
                return;
            }

            if (isArenaReadyForGameplay()) {
                gameContext.advanceGamePhaseToLobby();
                scheduleStartOfMatch();
                checkArenaReadinessTask.cancel();
            }
        }, 0, 40);
    }

    private void scheduleStartOfMatch() {
        final int NUM_SECONDS_BEFORE_STARTING_MATCH = 60;
        gameContext.getTwitchChat().sendMessageToChannel("Starting the round automatically in " + NUM_SECONDS_BEFORE_STARTING_MATCH + " seconds.");
        startCurrentMatchTask = Bukkit.getScheduler().runTaskLater(gameContext.getJavaPlugin(), this::startRounds, NUM_SECONDS_BEFORE_STARTING_MATCH * 20);
    }

    private boolean isArenaReadyForGameplay() {
        return haveNearbyChunksLoaded();
    }

    private void addWoolBorderToArena() {
        final World world = gameContext.getWorld();

        final int centerZ = location.getBlockZ();
        final int startX = getWestX();
        final int finalX = getEastX();
        // We are going to be facing south, which is the positive Z direction, so we only need to sample in that direction
        final int startZ = getNorthZ();
        final int finalZ = getSouthZ();


        Bukkit.getScheduler().runTask(gameContext.getJavaPlugin(), lambda -> {
            for (int x = startX; x <= finalX; x++) {
                for (int z = startZ; z <= finalZ; z++) {
                    if (x > startX && x < finalX && z != startZ && z != finalZ) continue;
                    if (z > startZ && z < finalZ && x != startX && x != finalX) continue;

                    final int highestBlockYAt = world.getHighestBlockYAt(x, z);
                    final Block block = world.getBlockAt(x, highestBlockYAt + 1, z);
                    // Note: the block is never null, it's just air when you go high enough.
                    block.setType(Material.YELLOW_WOOL);

                    final Block block2 = world.getBlockAt(x, highestBlockYAt + 2, z);
                    block2.setType(Material.LIGHT_BLUE_WOOL);
                }
            }
        });
    }

    /**
     * Tests whether all nearby chunks have loaded that may eventually have sheep in them.
     *
     * @return
     */
    private boolean haveNearbyChunksLoaded() {
        final World world = gameContext.getWorld();

        // This is how far away in any given direction from the center that we want to check
        final int maxSampleDistance = arenaSize;

        // Each chunk is 16x16, so we only need to sample every 16 blocks
        final int chunkSize = 16;
        // This is how many blocks we travel in between samples.
        final int centerX = location.getBlockX();
        final int centerZ = location.getBlockZ();
        final int startX = Math.floorDiv(centerX - maxSampleDistance, chunkSize);
        final int finalX = Math.floorDiv(centerX + maxSampleDistance, chunkSize);
        // We are going to be facing south, which is the positive Z direction, so we only need to sample in that direction
        final int startZ = Math.floorDiv(centerZ, chunkSize);
        final int finalZ = Math.floorDiv(centerZ + maxSampleDistance, chunkSize);
        for (int x = startX; x <= finalX; x++) {
            for (int z = startZ; z <= finalZ; z++) {
                // Chunk coordinates are just block coordinates divided by 16. So loading chunk (0, 0) would load blocks (0-15, 0-15).
                if (!world.isChunkLoaded(x, z)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * This function exists as part of the cleanup for the arena. Without this, it's possible (though unlikely) that one
     * arena is close enough to another and a named sheep dies even after resetting everything. That could mess up the
     * stats and even the game state.
     */
    public void killAllSheep() {
        Bukkit.getScheduler().runTask(gameContext.getJavaPlugin(), x -> {
            final Collection<GamePlayer> allPlayers = gameContext.getPlayers().getAllPlayers().values();
            for (final GamePlayer player :
                    allPlayers) {
                if (player.isSheepAlive()) {
                    player.getSheep().remove();
                }
            }
        });
    }

    /**
     * After the arena's location has been settled, this will move the player so that they're looking at it from the
     * north.
     */
    private void setCameraPositionToStartingLocation() {
        final World world = gameContext.getWorld();
        final int x = location.getBlockX();
        final int z = location.getBlockZ();
        final Block highestBlock = world.getHighestBlockAt(x, z);
        final int y = highestBlock.getLocation().getBlockY() + 42;
        final float yaw = 0;
        final float pitch = 55;

        Bukkit.getScheduler().runTask(gameContext.getJavaPlugin(), lambda -> gameContext.getFirstPlayer().teleport(new Location(world, x, y, z, yaw, pitch))
        );
    }

    public boolean isLocationInsideArena(final Location target) {
        final int blockX = target.getBlockX();
        final int blockZ = target.getBlockZ();
        return blockX >= location.getBlockX() - arenaSize && blockX <= location.getBlockX() + arenaSize && blockZ >= location.getBlockZ() && blockZ <= location.getBlockZ() + arenaSize;
    }

    /**
     * Returns if a biome is considered a bad location for the arena, e.g. it has too much water. Water isn't
     * necessarily a problem since we can cover liquids and hazards, but it's not a very easy arena to fight in. The one
     * ocean type that we DON'T consider bad just for some variety is Biome.FROZEN_OCEAN.
     *
     * @param biome
     * @return
     */
    private boolean isBiomeBad(final Biome biome) {
        final Biome[] badBiomes = {Biome.OCEAN, Biome.COLD_OCEAN, Biome.DEEP_COLD_OCEAN, Biome.DEEP_LUKEWARM_OCEAN, Biome.DEEP_OCEAN, Biome.WARM_OCEAN, Biome.LUKEWARM_OCEAN};
        return Util.arrayIncludes(badBiomes, biome);
    }

    /**
     * Given a location, this will sample blocks every so often to make sure that none of them are in a bad biome. This
     * is because the arena is essentially a large rectangular portion of the world, so no part of that rectangle should
     * be in, say, a watery biome.
     *
     * @param location
     * @return
     */
    private boolean isLocationValidForArena(final Location location) {
        final World world = location.getWorld();

        // This location
        final int blockX = location.getBlockX();
        final int blockY = location.getBlockY();
        final int blockZ = location.getBlockZ();

        // We'll just test the four corners of the arena and the center of it
        final Vector[] coordinates = {
                new Vector(blockX - arenaSize, blockY, blockZ),
                new Vector(blockX + arenaSize, blockY, blockZ),
                new Vector(blockX - arenaSize, blockY, blockZ + arenaSize),
                new Vector(blockX + arenaSize, blockY, blockZ + arenaSize),
                new Vector(blockX, blockY, blockZ + arenaSize / 2),
        };

        for (final Vector vector : coordinates) {
            // Sampling blocks in unloaded parts of the world is not very performant since Minecraft has to load
            // each chunk. I think it's better to use world.getBiome.
            final Biome biome = world.getBiome(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
            if (isBiomeBad((biome))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tries to find a good spot for the next arena. This picks a random portion in the world, ensures that it's "good",
     * and if not, retries the process.
     *
     * @param world
     * @return
     * @throws CannotLocateArenaException
     */
    private Location getNextArenaLocation(final World world) throws CannotLocateArenaException {
        final ThreadLocalRandom random = ThreadLocalRandom.current();

        final int maxNumAttempts = 1000;
        int numAttempts = 0;
        while (numAttempts++ < maxNumAttempts) {
            final Location randomLocation = new Location(world, random.nextDouble() * 10000000, 255, random.nextDouble() * 10000000);
            if (isLocationValidForArena(randomLocation)) {
                return randomLocation;
            }

        }
        throw new CannotLocateArenaException("Could not find a valid location within " + numAttempts + " attempts.");
    }

    /**
     * Creates a sheep for the given player. This is the player's avatar.
     *
     * @param gamePlayer
     * @param dyeColor
     * @return
     */
    protected Sheep createSheepForPlayer(final GamePlayer gamePlayer, final DyeColor dyeColor) {
        final World world = location.getWorld();
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final Location sheepLocation = location.clone();

        final int distance = arenaSize;
        sheepLocation.add(random.nextDouble() * distance * Util.getOneOrNegativeOne(), 0, random.nextDouble() * distance);
        final Block highestBlockAtSheepLocation = world.getHighestBlockAt(sheepLocation.getBlockX(), sheepLocation.getBlockZ());
        final Material highestBlockType = highestBlockAtSheepLocation.getType();

        // Prevent the player from dying as soon as they spawn by placing a non-flammable block above the hazard.
        if (highestBlockAtSheepLocation.isLiquid() || highestBlockType == Material.CACTUS || highestBlockType == Material.SWEET_BERRY_BUSH) {
            final Block blockAboveHighestBlock = world.getBlockAt(highestBlockAtSheepLocation.getX(), highestBlockAtSheepLocation.getY() + 1, highestBlockAtSheepLocation.getZ());
            blockAboveHighestBlock.setType(Material.CYAN_CONCRETE);
        }

        sheepLocation.setY(world.getHighestBlockYAt(sheepLocation.getBlockX(), sheepLocation.getBlockZ()) + 30);
        final Sheep sheep = (Sheep) world.spawnEntity(sheepLocation, EntityType.SHEEP);

        // We don't want sheep to have AI, but without AI, there's no fall damage, and we DO want fall damage. Instead,
        // we'll try adding a very slow potion.
        sheep.setAI(true);
        sheep.setCustomName(gamePlayer.getName());
        sheep.setCustomNameVisible(true);
        sheep.setRotation(90, 45);
        sheep.setAware(false);
        sheep.setGlowing(true);

        sheep.setColor(dyeColor);

        gamePlayer.setSheep(sheep);

        // "location" represents the center of the Arena, so we need to subtract the sheep distance. Then, since we're
        // facing south, the coordinates go from high to low, so we invert them, that way people see it as it is on the screen.
        final Vector relativeVector = getLocationRelativeToArena(sheepLocation);
        final int sheepNumber = gameContext.getPlayers().getNumLivingSheep();
        gameContext.getJavaPlugin().getServer().broadcastMessage("Sheep #" + sheepNumber + ": " + gamePlayer.getNameColoredForInGameChat() + ChatColor.RESET + " joined at " + ChatColor.LIGHT_PURPLE +
                "(" + relativeVector.getBlockX() + ", " + relativeVector.getBlockZ() + ") ");

        return sheep;
    }

    public int getLength() {
        return arenaSize * 2;
    }

    public int getDepth() {
        return arenaSize;
    }

    /**
     * Returns a vector whose coordinates will be in the range [0, dimension], where "dimension" is the length or depth
     * of the arena (based on which vector component you're looking at).
     *
     * @param target
     * @return
     */
    public Vector getLocationRelativeToArena(final Location target) {
        final int sheepX = (arenaSize * 2) - (target.getBlockX() - (location.getBlockX() - arenaSize));
        final int sheepZ = target.getBlockZ() - location.getBlockZ();

        return new Vector(sheepX, target.getBlockY(), sheepZ);
    }

    /**
     * Gets a string that says exactly where your sheep is located and then a human-readable form of that same string.
     *
     * @param sheepLocation
     * @return
     */
    public String getRelativeLocationInformationString(final Location sheepLocation) {
        final Vector locationRelativeToArena = getLocationRelativeToArena(sheepLocation);
        final String relativeLocationString = getHumanReadableRelativeLocation(sheepLocation);

        return String.format("Your sheep is located at (%d, %d). The arena is %dx%d, which means you are in the %s.", locationRelativeToArena.getBlockX(), locationRelativeToArena.getBlockZ(), getLength(), getDepth(), relativeLocationString);
    }

    /**
     * Gets a string like "top-left corner" that represents which of the 9 sections of the arena you're in.
     *
     * @param location
     * @return
     */
    public String getHumanReadableRelativeLocation(final Location location) {
        final Vector locationRelativeToArena = getLocationRelativeToArena(location);
        final int x = locationRelativeToArena.getBlockX();
        final int z = locationRelativeToArena.getBlockZ();

        // Split the whole arena into 9 equal portions and tell you which portion you fall under
        final int divisionLength = getLength() / 3;
        final int divisionDepth = getDepth() / 3;

        final String locationString;

        if (x < divisionLength && z < divisionDepth) {
            locationString = "bottom-left corner";
        } else if (x < divisionLength && z < 2 * divisionDepth) {
            locationString = "middle of the left side";
        } else if (x < divisionLength) {
            locationString = "top-left corner";
        } else if (x < 2 * divisionLength && z < divisionDepth) {
            locationString = "middle of the bottom side";
        } else if (x < 2 * divisionLength && z < divisionDepth * 2) {
            locationString = "middle of the arena";
        } else if (x < 2 * divisionLength) {
            locationString = "middle of the top side";
        } else if (z < divisionDepth) {
            locationString = "bottom-right corner";
        } else if (z < divisionDepth * 2) {
            locationString = "middle of the right side";
        } else {
            locationString = "top-right corner";
        }

        return locationString;
    }

    /**
     * This'll randomly spawn lava slightly above the arena, that way the match is greatly sped up.
     */
    public void placeLavaRandomly() {
        final World world = gameContext.getWorld();
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final int lavaX = random.nextInt(getWestX(), getEastX() + 1);
        final int lavaZ = random.nextInt(getNorthZ(), getSouthZ() + 1);

        Bukkit.getScheduler().runTask(gameContext.getJavaPlugin(), x -> {
            final int highestBlockY = world.getHighestBlockYAt(lavaX, lavaZ) + 5;
            final Block block = world.getBlockAt(lavaX, highestBlockY, lavaZ);
            block.setType(Material.LAVA);
        });
    }

    /**
     * Starts a round where sheep will take their turns.
     */
    public void startRound() {
        // Figure out everybody with TNT parameters and fire away
        final Collection<GamePlayer> allPlayers = gameContext.getPlayers().getAllPlayers().values();
        for (final GamePlayer player : allPlayers) {
            final Sheep sheep = player.getSheep();
            if (!sheep.isValid() || sheep.isDead() || !player.hasSetTntParameters()) {
                continue;
            }

            Bukkit.getScheduler().runTask(gameContext.getJavaPlugin(), x -> createTntForSheep(sheep, player.getTntNextYaw(), player.getTntNextPitch(), player.getTntNextPower(), player.getTntNextTtl())
            );
        }
    }

    /**
     * Fire TNT from all sheep.
     *
     * @param sheep
     * @param tntNextYaw
     * @param tntNextPitch
     * @param tntNextPower
     * @param tntNextTtl
     */
    private void createTntForSheep(final Sheep sheep, final int tntNextYaw, final int tntNextPitch, final int tntNextPower, final double tntNextTtl) {
        final TNTPrimed tnt = (TNTPrimed) sheep.getWorld().spawnEntity(sheep.getLocation(), EntityType.TNT);
        tnt.setFuseTicks((int) (tntNextTtl * Ticks.TICKS_PER_SECOND));

        // We want to be able to identify this TNT later, so we store a name, but we don't want the name to be visible
        tnt.setCustomName(sheep.getCustomName());

        tnt.setCustomNameVisible(false);

        final Vector tntVector = sheep.getLocation().getDirection();
        tntVector.multiply(tntNextPower / 25.0);
        tnt.setVelocity(tntVector);
    }

    public void startRounds() {
        startRoundIn(100);
        scheduleSuddenDeath();
        gameContext.advanceGamePhaseToPreGameplay();
    }

    private void scheduleSuddenDeath() {
        final int NUM_SECONDS_BEFORE_SUDDEN_DEATH = 5 * 60;
        final int numTicksBeforeSuddenDeath = NUM_SECONDS_BEFORE_SUDDEN_DEATH * Ticks.TICKS_PER_SECOND;

        suddenDeathTask = Bukkit.getScheduler().runTaskLater(gameContext.getJavaPlugin(), this::startSuddenDeath, numTicksBeforeSuddenDeath);
    }

    public void startSuddenDeath() {
        cancelSuddenDeath();
        gameContext.getTwitchChat().sendMessageToChannel("SUDDEN DEATH MODE ENGAGED! Lava will fall from the sky until all sheep are dead. ☠");

        // Repurpose the task into a periodic task that will spawn lava. By reusing the same variable, we will make
        // sure this gets canceled in the dispose function one way or another.
        suddenDeathTask = Bukkit.getScheduler().runTaskTimer(gameContext.getJavaPlugin(), this::placeLavaRandomly, 0, Ticks.TICKS_PER_SECOND / 2);
    }

    private void startRoundIn(final long delay) {
        // Make sure we've advanced to GAMEPLAY
        if (gameContext.getGamePhase() == GamePhase.PRE_GAMEPLAY) {
            gameContext.advanceGamePhaseToGameplay();
        }

        startNewMatchTask = Bukkit.getScheduler().runTaskLater(gameContext.getJavaPlugin(), () -> {
            gameContext.getArena().startRound();
            startRoundIn(timeBetweenTurns);

            // The minimum possible turn time is 1 second
            timeBetweenTurns = Math.max(20, (int) Math.floor(timeBetweenTurns * 0.9));
        }, delay);
    }

    public void setStartingNumSheep(final int numLivingSheep) {
        this.startingNumSheep = numLivingSheep;
    }

    public int getStartingNumSheep() {
        return startingNumSheep;
    }
}
