package live.adamlearns.animalroyale;

import java.util.Arrays;

public final class Util {
    // Don't allow construction of this class since it only has static members.
    private Util() {
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
