package fr.minlego.redstonevisualizer.update;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Routes successful client-side block-state transitions to one listener per
 * world.
 *
 * <p>The tracker is deliberately world-scoped. Callers should enable it again
 * for a newly created {@link ClientWorld}; the weak keys avoid retaining a
 * disconnected world.</p>
 */
public final class BlockStateUpdateTracker {
    private static final Map<ClientWorld, Tracker> TRACKERS = new WeakHashMap<>();

    private BlockStateUpdateTracker() {
    }

    @FunctionalInterface
    public interface Listener {
        void onBlockStateChange(BlockPos position, BlockState oldState, BlockState newState);
    }

    /** Starts observing this world and drops any transition still in flight. */
    public static void enable(ClientWorld world, Listener listener) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(listener, "listener");
        Tracker tracker = TRACKERS.computeIfAbsent(world, ignored -> new Tracker());
        tracker.listener = listener;
        tracker.enabled = true;
        tracker.pending.clear();
    }

    /** Stops observing this world while keeping its small world-local tracker. */
    public static void disable(ClientWorld world) {
        Objects.requireNonNull(world, "world");
        Tracker tracker = TRACKERS.get(world);
        if (tracker != null) {
            tracker.enabled = false;
            tracker.pending.clear();
        }
    }

    /** Clears an in-flight transition without changing the enabled state. */
    public static void clear(ClientWorld world) {
        Objects.requireNonNull(world, "world");
        Tracker tracker = TRACKERS.get(world);
        if (tracker != null) {
            tracker.pending.clear();
        }
    }

    public static void begin(ClientWorld world, BlockPos position) {
        Tracker tracker = TRACKERS.get(world);
        if (tracker == null || !tracker.enabled) {
            return;
        }

        BlockPos immutablePosition = position.toImmutable();
        if (!world.isPosLoaded(immutablePosition)) {
            tracker.pending.push(Pending.ignored());
            return;
        }

        tracker.pending.push(new Pending(
                immutablePosition,
                world.getBlockState(immutablePosition),
                true));
    }

    public static void finish(ClientWorld world, boolean succeeded) {
        Tracker tracker = TRACKERS.get(world);
        if (tracker == null || tracker.pending.isEmpty()) {
            return;
        }

        Pending pending = tracker.pending.pop();
        if (!succeeded || !tracker.enabled || !pending.observed) {
            return;
        }
        BlockState after = world.getBlockState(pending.position);
        if (pending.oldState.equals(after)) {
            return;
        }
        tracker.listener.onBlockStateChange(
                pending.position, pending.oldState, after);
    }

    private static final class Tracker {
        private final Deque<Pending> pending = new ArrayDeque<>();
        private Listener listener;
        private boolean enabled;
    }

    private record Pending(
            BlockPos position,
            BlockState oldState,
            boolean observed) {
        private static Pending ignored() {
            return new Pending(null, null, false);
        }
    }
}
