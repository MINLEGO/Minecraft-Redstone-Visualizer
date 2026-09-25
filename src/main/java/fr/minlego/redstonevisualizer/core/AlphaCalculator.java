package fr.minlego.redstonevisualizer.core;

/** Pure tick-based opacity calculation. Percentages are in the range 0..100. */
public final class AlphaCalculator {
    private AlphaCalculator() {
    }

    public static double alphaPercent(int baseOpacity, long elapsedTicks,
            long durationTicks, long fadeTicks) {
        validate(baseOpacity, durationTicks, fadeTicks);
        if (elapsedTicks < 0) {
            throw new IllegalArgumentException("elapsedTicks must be non-negative");
        }
        if (durationTicks == 0 || elapsedTicks >= durationTicks) {
            return baseOpacity;
        }
        if (fadeTicks == 0) {
            return 100;
        }

        long fadeStart = durationTicks - fadeTicks;
        if (elapsedTicks < fadeStart) {
            return 100;
        }
        double progress = (double) (elapsedTicks - fadeStart) / fadeTicks;
        return 100 - (100 - baseOpacity) * progress;
    }

    public static int alpha(int baseOpacity, long elapsedTicks,
            long durationTicks, long fadeTicks) {
        return (int) Math.round(alphaPercent(baseOpacity, elapsedTicks,
                durationTicks, fadeTicks));
    }

    private static void validate(int baseOpacity, long durationTicks, long fadeTicks) {
        if (baseOpacity < 0 || baseOpacity > 100) {
            throw new IllegalArgumentException("baseOpacity must be between 0 and 100");
        }
        if (durationTicks < 0) {
            throw new IllegalArgumentException("durationTicks must be non-negative");
        }
        if (fadeTicks < 0 || fadeTicks > durationTicks) {
            throw new IllegalArgumentException("fadeTicks must be between 0 and durationTicks");
        }
    }
}
