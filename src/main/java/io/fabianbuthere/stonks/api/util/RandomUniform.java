package io.fabianbuthere.stonks.api.util;

import java.util.concurrent.ThreadLocalRandom;

public final class RandomUniform {
    private RandomUniform() { }

    public static int between(int minInclusive, int maxExclusive) {
        return ThreadLocalRandom.current().nextInt(minInclusive, maxExclusive);
    }

    public static int between(int minInclusive, int maxInclusive, int step) {
        if (step <= 0) return between(minInclusive, maxInclusive + 1);

        return minInclusive + step * ThreadLocalRandom.current().nextInt(
                (maxInclusive - minInclusive) / step + 1
        );
    }
}
