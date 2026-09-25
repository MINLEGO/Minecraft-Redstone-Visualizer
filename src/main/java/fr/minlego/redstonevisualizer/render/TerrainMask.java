package fr.minlego.redstonevisualizer.render;

import fr.minlego.redstonevisualizer.core.AlphaTracker;
import fr.minlego.redstonevisualizer.core.Zone;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;

/** Immutable settings snapshot read by Minecraft's section build workers. */
public final class TerrainMask {
    private static volatile Settings settings = new Settings(null, null, false, 100,
            null, Set.of(), 0);

    private TerrainMask() {
    }

    public static void configure(Zone zone, String currentDimension, boolean enabled,
            int opacityPercent) {
        if (opacityPercent < 0 || opacityPercent > 100) {
            throw new IllegalArgumentException("Opacity must be 0..100");
        }
        settings = new Settings(zone, currentDimension, enabled, opacityPercent,
                null, Set.of(), 0);
    }

    public static void configure(Zone zone, String currentDimension, boolean enabled,
            AlphaTracker tracker, Set<String> whitelist, long tick) {
        settings = new Settings(zone, currentDimension, enabled, tracker.baseOpacity(),
                tracker, Set.copyOf(whitelist), tick);
    }

    public static int opacityAt(BlockPos position) {
        return opacityAt(position, null);
    }

    public static int opacityAt(BlockPos position, BlockState state) {
        Settings snapshot = settings;
        if (!contains(snapshot, position)) {
            return 255;
        }
        if (state != null && snapshot.whitelist.contains(
                Registries.BLOCK.getId(state.getBlock()).toString())) {
            return 255;
        }
        double percent = snapshot.tracker == null ? snapshot.opacityPercent
                : snapshot.tracker.alphaPercent(new fr.minlego.redstonevisualizer.core.BlockPos(
                        position.getX(), position.getY(), position.getZ()), snapshot.tick);
        return (int) Math.round(percent * 255 / 100);
    }

    public static boolean mayMask(BlockPos position) {
        return contains(settings, position);
    }

    public static boolean isHighlighted(BlockPos position, BlockState state) {
        Settings snapshot = settings;
        if (!contains(snapshot, position)) {
            return false;
        }
        if (snapshot.whitelist.contains(Registries.BLOCK.getId(state.getBlock()).toString())) {
            return true;
        }
        return snapshot.tracker != null && snapshot.tracker.isActive(
                new fr.minlego.redstonevisualizer.core.BlockPos(
                        position.getX(), position.getY(), position.getZ()), snapshot.tick);
    }

    private static boolean contains(Settings snapshot, BlockPos position) {
        Zone zone = snapshot.zone;
        return snapshot.enabled && zone != null && zone.dimension().equals(snapshot.dimension)
                && position.getX() >= zone.minX() && position.getX() <= zone.maxX()
                && position.getY() >= zone.minY() && position.getY() <= zone.maxY()
                && position.getZ() >= zone.minZ() && position.getZ() <= zone.maxZ();
    }

    private record Settings(Zone zone, String dimension, boolean enabled,
            int opacityPercent, AlphaTracker tracker, Set<String> whitelist, long tick) {
    }
}
