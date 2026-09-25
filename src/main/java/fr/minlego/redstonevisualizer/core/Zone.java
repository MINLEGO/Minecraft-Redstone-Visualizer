package fr.minlego.redstonevisualizer.core;

import java.util.Objects;
import java.util.OptionalLong;

/** An inclusive rectangular zone in one dimension. */
public record Zone(String dimension, BlockPos corner1, BlockPos corner2) {
    public Zone {
        Objects.requireNonNull(dimension, "dimension");
        if (dimension.isBlank()) {
            throw new IllegalArgumentException("dimension must not be blank");
        }
        Objects.requireNonNull(corner1, "corner1");
        Objects.requireNonNull(corner2, "corner2");
    }

    public boolean contains(BlockPos position) {
        Objects.requireNonNull(position, "position");
        return position.x() >= minX() && position.x() <= maxX()
                && position.y() >= minY() && position.y() <= maxY()
                && position.z() >= minZ() && position.z() <= maxZ();
    }

    public boolean contains(String currentDimension, BlockPos position) {
        return dimension.equals(currentDimension) && contains(position);
    }

    public int minX() {
        return Math.min(corner1.x(), corner2.x());
    }

    public int maxX() {
        return Math.max(corner1.x(), corner2.x());
    }

    public int minY() {
        return Math.min(corner1.y(), corner2.y());
    }

    public int maxY() {
        return Math.max(corner1.y(), corner2.y());
    }

    public int minZ() {
        return Math.min(corner1.z(), corner2.z());
    }

    public int maxZ() {
        return Math.max(corner1.z(), corner2.z());
    }

    public long width() {
        return (long) maxX() - minX() + 1;
    }

    public long height() {
        return (long) maxY() - minY() + 1;
    }

    public long depth() {
        return (long) maxZ() - minZ() + 1;
    }

    /**
     * Returns the number of positions, saturated at {@link Long#MAX_VALUE}
     * when the mathematical product does not fit in a long.
     */
    public long size() {
        return exactSize().orElse(Long.MAX_VALUE);
    }

    public boolean sizeOverflow() {
        return exactSize().isEmpty();
    }

    public OptionalLong exactSize() {
        try {
            long xy = Math.multiplyExact(width(), height());
            return OptionalLong.of(Math.multiplyExact(xy, depth()));
        } catch (ArithmeticException overflow) {
            return OptionalLong.empty();
        }
    }

    public boolean exceeds(long threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("threshold must be non-negative");
        }
        return sizeOverflow() || size() > threshold;
    }
}
