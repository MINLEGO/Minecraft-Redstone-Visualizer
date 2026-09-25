package fr.minlego.redstonevisualizer.world;

import fr.minlego.redstonevisualizer.core.BlockPos;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** A sidecar file in the save directory; it never changes Minecraft world data. */
public final class WorldStore {
    private static final String FILE_NAME = "redstone_visualizer.properties";

    private WorldStore() {
    }

    public static WorldState load(Path saveDirectory) throws IOException {
        Path file = saveDirectory.resolve(FILE_NAME);
        if (!Files.isRegularFile(file)) {
            return WorldState.EMPTY;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        return new WorldState(Boolean.parseBoolean(properties.getProperty("enabled", "false")),
                readCorner(properties, "first"), readCorner(properties, "second"));
    }

    public static void save(Path saveDirectory, WorldState state) throws IOException {
        Files.createDirectories(saveDirectory);
        Properties properties = new Properties();
        properties.setProperty("enabled", Boolean.toString(state.enabled()));
        writeCorner(properties, "first", state.first());
        writeCorner(properties, "second", state.second());

        Path file = saveDirectory.resolve(FILE_NAME);
        Path temporary = Files.createTempFile(saveDirectory, "redstone_visualizer-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "Redstone Visualizer per-world state");
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static WorldState.Corner readCorner(Properties properties, String prefix) {
        try {
            String dimension = properties.getProperty(prefix + ".dimension");
            BlockPos position = new BlockPos(
                    Integer.parseInt(properties.getProperty(prefix + ".x")),
                    Integer.parseInt(properties.getProperty(prefix + ".y")),
                    Integer.parseInt(properties.getProperty(prefix + ".z")));
            return new WorldState.Corner(dimension, position);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static void writeCorner(Properties properties, String prefix, WorldState.Corner corner) {
        if (corner == null) {
            return;
        }
        properties.setProperty(prefix + ".dimension", corner.dimension());
        properties.setProperty(prefix + ".x", Integer.toString(corner.position().x()));
        properties.setProperty(prefix + ".y", Integer.toString(corner.position().y()));
        properties.setProperty(prefix + ".z", Integer.toString(corner.position().z()));
    }
}
