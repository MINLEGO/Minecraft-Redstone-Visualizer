package fr.minlego.redstonevisualizer.core;

import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Stores the last update tick for positions and derives their opacity. */
public final class AlphaTracker {
    public static final int DEFAULT_BASE_OPACITY = 0;
    public static final long DEFAULT_DURATION_TICKS = 12;
    public static final long DEFAULT_FADE_TICKS = 4;

    private final Map<BlockPos, Long> lastUpdates = new ConcurrentHashMap<>();
    private volatile int baseOpacity;
    private volatile long durationTicks;
    private volatile long fadeTicks;

    public AlphaTracker() {
        this(DEFAULT_BASE_OPACITY, DEFAULT_DURATION_TICKS, DEFAULT_FADE_TICKS);
    }

    public AlphaTracker(int baseOpacity, long durationTicks, long fadeTicks) {
        configure(baseOpacity, durationTicks, fadeTicks);
    }

    public void configure(int baseOpacity, long durationTicks, long fadeTicks) {
        AlphaCalculator.alphaPercent(baseOpacity, 0, durationTicks, fadeTicks);
        this.baseOpacity = baseOpacity;
        this.durationTicks = durationTicks;
        this.fadeTicks = fadeTicks;
    }

    public int baseOpacity() {
        return baseOpacity;
    }

    public long durationTicks() {
        return durationTicks;
    }

    public long fadeTicks() {
        return fadeTicks;
    }

    public void recordUpdate(BlockPos position, long gameTick) {
        Objects.requireNonNull(position, "position");
        requireTick(gameTick);
        lastUpdates.put(position, gameTick);
    }

    /** Records only a real state transition and returns whether it was recorded. */
    public boolean recordChange(BlockPos position, Object previousState,
            Object currentState, long gameTick) {
        if (Objects.equals(previousState, currentState)) {
            return false;
        }
        recordUpdate(position, gameTick);
        return true;
    }

    public OptionalLong lastUpdate(BlockPos position) {
        Objects.requireNonNull(position, "position");
        Long tick = lastUpdates.get(position);
        return tick == null ? OptionalLong.empty() : OptionalLong.of(tick);
    }

    public boolean isActive(BlockPos position, long currentTick) {
        Objects.requireNonNull(position, "position");
        requireTick(currentTick);
        Long lastUpdate = lastUpdates.get(position);
        return lastUpdate != null && elapsed(lastUpdate, currentTick) < durationTicks;
    }

    public double alphaPercent(BlockPos position, long currentTick) {
        Objects.requireNonNull(position, "position");
        requireTick(currentTick);
        Long lastUpdate = lastUpdates.get(position);
        if (lastUpdate == null) {
            return baseOpacity;
        }
        long duration = durationTicks;
        return AlphaCalculator.alphaPercent(baseOpacity,
                elapsed(lastUpdate, currentTick), duration, Math.min(fadeTicks, duration));
    }

    public double alphaPercent(BlockPos position, String blockId,
            long currentTick, BlockWhitelist whitelist) {
        Objects.requireNonNull(whitelist, "whitelist");
        return whitelist.contains(blockId) ? 100 : alphaPercent(position, currentTick);
    }

    public int alpha(BlockPos position, long currentTick) {
        return (int) Math.round(alphaPercent(position, currentTick));
    }

    public int alpha(BlockPos position, String blockId,
            long currentTick, BlockWhitelist whitelist) {
        return (int) Math.round(alphaPercent(position, blockId, currentTick, whitelist));
    }

    /** Removes updates whose full active window has elapsed. */
    public int purge(long currentTick) {
        requireTick(currentTick);
        int before = lastUpdates.size();
        lastUpdates.entrySet().removeIf(entry ->
                elapsed(entry.getValue(), currentTick) >= durationTicks);
        return before - lastUpdates.size();
    }

    public void clear() {
        lastUpdates.clear();
    }

    public int trackedCount() {
        return lastUpdates.size();
    }

    public Set<BlockPos> trackedPositions() {
        return Set.copyOf(lastUpdates.keySet());
    }

    private static long elapsed(long lastUpdate, long currentTick) {
        if (currentTick <= lastUpdate) {
            return 0;
        }
        long elapsed = currentTick - lastUpdate;
        return elapsed < 0 ? Long.MAX_VALUE : elapsed;
    }

    private static void requireTick(long tick) {
        if (tick < 0) {
            throw new IllegalArgumentException("gameTick must be non-negative");
        }
    }
}
