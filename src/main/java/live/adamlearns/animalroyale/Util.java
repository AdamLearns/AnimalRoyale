package live.adamlearns.animalroyale;

import org.bukkit.Location;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public final class Util {
    // Don't allow construction of this class since it only has static members.
    private Util() {
    }

    /**
     * @return Either 1 or -1, chosen randomly.
     */
    public static int getOneOrNegativeOne() {
        return ThreadLocalRandom.current().nextBoolean() ? -1 : 1;
    }

    /**
     * Randomly get a single element from the specified array.
     *
     * @param array
     * @param <T>
     * @return
     */
    public static <T> T sampleArray(final T[] array) {
        final int rnd = ThreadLocalRandom.current().nextInt(array.length);
        return array[rnd];
    }

    public static <T extends Comparable<T>> T clamp(final T numberToClamp, final T min, final T max) {
        return numberToClamp.compareTo(min) < 0 ? min : numberToClamp.compareTo(max) > 0 ? max : numberToClamp;
    }

    /**
     * Locations that are at the edge of a block may cause entities to suffocate in nearby blocks. To fix this, you
     * apparently just pick the center of the block as mentioned <a href="https://www.spigotmc.org/threads/teleporting-player-to-center-of-a-block.255699/?__cf_chl_jschl_tk__=4e2c930c0179625dd750d46b94c5c116f2d94707-1583359206-0-AWv8EYIRfuycEC86A">here</a>
     *
     * @param location
     */
    public static void setLocationToCenterOfBlock(final Location location) {
        location.setX(Math.floor(location.getX()) + 0.5);
        location.setZ(Math.floor(location.getZ()) + 0.5);
    }

    /**
     * Checks to see if the specified array contains the given value.
     *
     * @param array
     * @param value
     * @param <T>
     * @return
     */
    public static <T> boolean arrayIncludes(final T[] array, final T value) {
        return Arrays.asList(array).contains(value);
    }
}
