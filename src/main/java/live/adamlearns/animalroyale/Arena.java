package live.adamlearns.animalroyale;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
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
     * The distance from this arena's location that a sheep could spawn.
     */
    private final int sheepDistanceFromLocation = 40;

    /**
     * This is in ticks.
     */
    private int timeBetweenTurns = 20 * 20;

    // This represents the top-center of the arena (since we're looking south).
    private Location location;

    /**
     * We periodically check if the arena is ready to have players join. When it is, this task will cancel itself.
     */
    private BukkitTask checkArenaReadinessTask;
    private int startingNumSheep;

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

        deleteNonPlayerSheep();

        addWoolBorderToArena();

        setupArenaReadinessCheck();
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
                checkArenaReadinessTask.cancel();
            }
        }, 0, 40);
    }

    private boolean isArenaReadyForGameplay() {
        return haveNearbyChunksLoaded();
    }

    private void addWoolBorderToArena() {
        final World world = gameContext.getWorld();

        final int centerX = location.getBlockX();
        final int centerZ = location.getBlockZ();
        final int startX = centerX - sheepDistanceFromLocation;
        final int finalX = centerX + sheepDistanceFromLocation;
        // We are going to be facing south, which is the positive Z direction, so we only need to sample in that direction
        @SuppressWarnings("UnnecessaryLocalVariable") final int startZ = centerZ;
        final int finalZ = centerZ + sheepDistanceFromLocation;

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
    }

    /**
     * Tests whether all nearby chunks have loaded that may eventually have sheep in them.
     *
     * @return
     */
    private boolean haveNearbyChunksLoaded() {
        final World world = gameContext.getWorld();

        // This is how far away in any given direction from the center that we want to check
        final int maxSampleDistance = sheepDistanceFromLocation;

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
     * This deletes any sheep that may have already existed in the world at the arena location.
     * <p>
     * I'm pretty sure this function doesn't actually work, but I haven't figured out why.
     */
    private void deleteNonPlayerSheep() {
        final World world = gameContext.getWorld();
        final Location deleteSheepLocation = location.clone();
        final Block highestBlock = world.getHighestBlockAt(location.getBlockX(), location.getBlockZ());
        deleteSheepLocation.setY(highestBlock.getLocation().getBlockY());
        final Collection<Entity> nearbyEntities = world.getNearbyEntities(deleteSheepLocation, 100, 100, 100, E -> E.getType() == EntityType.SHEEP);
        for (final Entity entity : nearbyEntities) {
            entity.remove();
        }
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
        final int y = highestBlock.getLocation().getBlockY() + 20;
        final float yaw = 0;
        final float pitch = 45;
        gameContext.getFirstPlayer().teleport(new Location(world, x, y, z, yaw, pitch));
    }

    public boolean isLocationInsideArena(final Location target) {
        final int blockX = target.getBlockX();
        final int blockZ = target.getBlockZ();
        return blockX >= location.getBlockX() - sheepDistanceFromLocation && blockX <= location.getBlockX() + sheepDistanceFromLocation && blockZ >= location.getBlockZ() && blockZ <= location.getBlockZ() + sheepDistanceFromLocation;
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
        final Biome[] badBiomes = {Biome.OCEAN, Biome.COLD_OCEAN, Biome.DEEP_COLD_OCEAN, Biome.DEEP_FROZEN_OCEAN, Biome.DEEP_LUKEWARM_OCEAN, Biome.DEEP_OCEAN, Biome.DEEP_WARM_OCEAN, Biome.LUKEWARM_OCEAN, Biome.WARM_OCEAN};
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

        // This is how far away in any given direction from the center that we want to check
        final int maxSampleDistance = 100;

        // This is how many blocks we travel in between samples.
        final int sampleInterval = 100;
        final int centerX = location.getBlockX();
        final int centerZ = location.getBlockZ();
        final int startX = centerX - maxSampleDistance;
        final int finalX = centerX + maxSampleDistance;
        // We are going to be facing south, which is the positive Z direction, so we only need to sample in that direction
        @SuppressWarnings("UnnecessaryLocalVariable") final int startZ = centerZ;
        final int finalZ = centerZ + maxSampleDistance * 2;
        for (int x = startX; x <= finalX; x += sampleInterval) {
            for (int z = startZ; z <= finalZ; z += sampleInterval) {
                // Sampling blocks in unloaded parts of the world is not very performant since Minecraft has to load
                // each chunk. I think it's better to use world.getBiome.

                final Biome biome = world.getBiome(x, location.getBlockY(), z);
                if (isBiomeBad((biome))) {
                    return false;
                }
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
    protected Vector createSheepForPlayer(final GamePlayer gamePlayer, final DyeColor dyeColor) {
        final World world = location.getWorld();
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final Location sheepLocation = location.clone();

        final int distance = sheepDistanceFromLocation;
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
        final int sheepX = (sheepDistanceFromLocation * 2) - (sheepLocation.getBlockX() - (location.getBlockX() - sheepDistanceFromLocation));
        final int sheepZ = sheepLocation.getBlockZ() - location.getBlockZ();
        final int numPlayers = gameContext.getPlayers().getAllPlayers().size();
        gameContext.getJavaPlugin().getServer().broadcastMessage("Sheep #" + numPlayers + ": " + gamePlayer.getNameColoredForInGameChat() + ChatColor.RESET + " joined at " + ChatColor.LIGHT_PURPLE +
                "(" + sheepX + ", " + sheepZ + ") ");

        return new Vector(sheepX, sheepLocation.getBlockY(), sheepZ);
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
        final TNTPrimed tnt = (TNTPrimed) sheep.getWorld().spawnEntity(sheep.getLocation(), EntityType.PRIMED_TNT);
        tnt.setFuseTicks((int) (tntNextTtl * 20));

        // We want to be able to identify this TNT later, so we store a name, but we don't want the name to be visible
        tnt.setCustomName(sheep.getCustomName());

        tnt.setCustomNameVisible(false);

        final Vector tntVector = sheep.getLocation().getDirection();
        tntVector.multiply(tntNextPower / 25.0);
        tnt.setVelocity(tntVector);
    }

    public void startRounds() {
        startRoundIn(100);
        gameContext.advanceGamePhaseToPreGameplay();
    }

    private void startRoundIn(final long delay) {
        // Make sure we've advanced to GAMEPLAY
        if (gameContext.getGamePhase() == GamePhase.PRE_GAMEPLAY) {
            gameContext.advanceGamePhaseToGameplay();
        }

        Bukkit.getScheduler().runTaskLater(gameContext.getJavaPlugin(), () -> {
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
