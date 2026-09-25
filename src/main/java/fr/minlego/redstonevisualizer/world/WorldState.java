package fr.minlego.redstonevisualizer.world;

import fr.minlego.redstonevisualizer.core.BlockPos;
import fr.minlego.redstonevisualizer.core.Zone;
import java.util.Optional;

/** The settings tied to a particular single-player save or multiplayer server. */
public record WorldState(boolean enabled, Corner first, Corner second) {
    public static final WorldState EMPTY = new WorldState(false, null, null);

    public Optional<Zone> zone() {
        if (first == null || second == null || !first.dimension().equals(second.dimension())) {
            return Optional.empty();
        }
        return Optional.of(new Zone(first.dimension(), first.position(), second.position()));
    }

    public WorldState withEnabled(boolean value) {
        return new WorldState(value, first, second);
    }

    public WorldState withFirst(Corner value) {
        return new WorldState(enabled, value, second);
    }

    public WorldState withSecond(Corner value) {
        return new WorldState(enabled, first, value);
    }

    public record Corner(String dimension, BlockPos position) {
        public Corner {
            if (dimension == null || !dimension.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) {
                throw new IllegalArgumentException("Invalid dimension: " + dimension);
            }
            if (position == null) {
                throw new IllegalArgumentException("Missing position");
            }
        }
    }
}
