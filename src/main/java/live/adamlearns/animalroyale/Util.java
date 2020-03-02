package live.adamlearns.animalroyale;

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
