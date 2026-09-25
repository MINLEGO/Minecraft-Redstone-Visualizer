package fr.minlego.redstonevisualizer.world;

import fr.minlego.redstonevisualizer.core.BlockPos;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WorldStoreTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("redstone-visualizer-store-test-");
        if (!WorldStore.load(directory).equals(WorldState.EMPTY)) {
            throw new AssertionError("A new world must start disabled");
        }
        WorldState state = WorldState.EMPTY
                .withFirst(new WorldState.Corner("minecraft:overworld", new BlockPos(-2, 64, 3)))
                .withSecond(new WorldState.Corner("minecraft:overworld", new BlockPos(4, 68, 9)))
                .withEnabled(true);
        WorldStore.save(directory, state);
        if (!WorldStore.load(directory).equals(state) || WorldStore.load(directory).zone().isEmpty()) {
            throw new AssertionError("World state did not survive a save/load cycle");
        }
        Files.writeString(directory.resolve("redstone_visualizer.properties"),
                "enabled=true\nfirst.dimension=invalid dimension\nfirst.x=broken\n");
        if (WorldStore.load(directory).zone().isPresent()) {
            throw new AssertionError("Corrupt coordinates must not activate a zone");
        }
    }
}
