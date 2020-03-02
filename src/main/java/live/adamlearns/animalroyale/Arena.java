package live.adamlearns.animalroyale;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Sheep;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

public class Arena {
    private final GameContext gameContext;

    /**
     * The distance from this arena's location that a sheep could spawn.
     */
    private final int sheepDistanceFromLocation = 40;

    // This represents the top-center of the arena (since we're looking south).
    private Location location;

    /**
     * We periodically check if the arena is ready to have players join. When it is, this task will cancel itself.
     */
    private BukkitTask checkArenaReadinessTask;

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

        setupArenaReadinessCheck();
    }

    /**
     * Checks every so often to see if the arena is ready, and when it is, this will advance the game state.
     */
    private void setupArenaReadinessCheck() {
        checkArenaReadinessTask = Bukkit.getScheduler().runTaskTimer(gameContext.getJavaPlugin(), () -> {
            if (isArenaReadyForGameplay()) {
                gameContext.advanceGamePhaseToLobby();
                checkArenaReadinessTask.cancel();
            }
        }, 0, 20);
    }

    private boolean isArenaReadyForGameplay() {
        return haveNearbyChunksLoaded();
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

    /**
     * Returns if a biome is considered a bad location for the arena, e.g. it has too much water.
     *
     * @param biome
     * @return
     */
    private boolean isBiomeBad(final Biome biome) {
        final Biome[] badBiomes = {Biome.OCEAN, Biome.COLD_OCEAN, Biome.DEEP_COLD_OCEAN, Biome.DEEP_FROZEN_OCEAN, Biome.DEEP_LUKEWARM_OCEAN, Biome.DEEP_OCEAN, Biome.DEEP_WARM_OCEAN, Biome.FROZEN_OCEAN, Biome.LUKEWARM_OCEAN, Biome.WARM_OCEAN};
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
     * @param senderName
     * @param dyeColor
     */
    protected void createSheepForPlayer(final String senderName, final DyeColor dyeColor) {
        final World world = location.getWorld();
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final Location sheepLocation = location.clone();

        final int distance = sheepDistanceFromLocation;
        sheepLocation.add(random.nextDouble() * distance * Util.getOneOrNegativeOne(), 0, random.nextDouble() * distance);
        sheepLocation.setY(world.getHighestBlockYAt(sheepLocation.getBlockX(), sheepLocation.getBlockZ()) + 30);
        final Sheep sheep = (Sheep) world.spawnEntity(sheepLocation, EntityType.SHEEP);

        // We don't want sheep to have AI, but without AI, there's no fall damage, and we DO want fall damage. Instead,
        // we'll try adding a very slow potion.
        sheep.setAI(true);
        sheep.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, Integer.MAX_VALUE));
        sheep.setCustomName(senderName);
        sheep.setCustomNameVisible(true);
        sheep.setRotation(90, 90);
        sheep.setGlowing(true);

        sheep.setColor(dyeColor);
    }
}
