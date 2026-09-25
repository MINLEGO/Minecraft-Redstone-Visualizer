package fr.minlego.redstonevisualizer;

import fr.minlego.redstonevisualizer.config.RedstoneVisualizerConfig;
import fr.minlego.redstonevisualizer.core.AlphaTracker;
import fr.minlego.redstonevisualizer.core.Zone;
import fr.minlego.redstonevisualizer.render.TerrainMask;
import fr.minlego.redstonevisualizer.update.BlockStateUpdateTracker;
import fr.minlego.redstonevisualizer.world.WorldState;
import fr.minlego.redstonevisualizer.world.WorldStore;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Connects one local or remote world to its local visualizer state. */
public final class VisualizerSession {
    private static final Logger LOGGER = LoggerFactory.getLogger("redstone_visualizer");

    private final MinecraftClient client;
    private final AlphaTracker updates = new AlphaTracker();
    private ClientWorld world;
    private Path stateDirectory;
    private WorldState state = WorldState.EMPTY;
    private long tick;
    private int lastServerTick;
    private int opacity = -1;
    private int duration = -1;
    private int fade = -1;
    private List<String> whitelist = List.of();

    public VisualizerSession(MinecraftClient client) {
        this.client = client;
    }

    public WorldState state() {
        return state;
    }

    public boolean isWorldAvailable() {
        return world != null && stateDirectory != null;
    }

    public void onClientTick() {
        if (client.world != world) {
            changeWorld(client.world);
        }
        syncConfig();
        advanceGameClock();
    }

    /** Solo follows the integrated server; multiplayer follows client game ticks. */
    private void advanceGameClock() {
        if (!isWorldAvailable()) {
            return;
        }
        long previousTick = tick;
        if (client.getServer() != null) {
            int serverTick = client.getServer().getTicks();
            if (serverTick == lastServerTick) {
                return;
            }
            tick += Integer.toUnsignedLong(serverTick - lastServerTick);
            lastServerTick = serverTick;
        } else {
            tick++;
        }
        if (state.enabled() && state.zone().isPresent()) {
            for (fr.minlego.redstonevisualizer.core.BlockPos position : updates.trackedPositions()) {
                if (updates.alphaPercent(position, previousTick)
                        != updates.alphaPercent(position, tick)) {
                    rebuildAround(position);
                }
            }
        }
        updates.purge(tick);
        publish();
    }

    public void toggle() {
        if (!isWorldAvailable()) {
            return;
        }
        boolean wasEnabled = state.enabled();
        state = state.withEnabled(!wasEnabled);
        if (!wasEnabled) {
            updates.clear();
        }
        observe();
        save();
        rebuild(state.zone().orElse(null));
        publish();
    }

    public void setCorner(int index, WorldState.Corner corner) {
        if (!isWorldAvailable() || (index != 1 && index != 2)) {
            return;
        }
        Zone previous = state.zone().orElse(null);
        if (index == 1) {
            WorldState.Corner other = state.second();
            state = new WorldState(state.enabled(), corner,
                    other != null && other.dimension().equals(corner.dimension()) ? other : null);
        } else {
            WorldState.Corner other = state.first();
            state = new WorldState(state.enabled(),
                    other != null && other.dimension().equals(corner.dimension()) ? other : null, corner);
        }
        updates.clear();
        save();
        rebuild(previous);
        rebuild(state.zone().orElse(null));
        publish();
    }

    private void changeWorld(ClientWorld next) {
        if (world != null) {
            BlockStateUpdateTracker.disable(world);
        }
        Zone previous = state.zone().orElse(null);
        world = next;
        stateDirectory = stateDirectory(next);
        state = WorldState.EMPTY;
        if (world != null && stateDirectory != null) {
            try {
                state = WorldStore.load(stateDirectory);
            } catch (IOException exception) {
                LOGGER.warn("Could not load world visualizer settings", exception);
            }
        }
        updates.clear();
        tick = 0;
        lastServerTick = client.getServer() == null ? 0 : client.getServer().getTicks();
        observe();
        rebuild(previous);
        rebuild(state.zone().orElse(null));
        publish();
    }

    private void observe() {
        if (isWorldAvailable() && state.enabled()) {
            BlockStateUpdateTracker.enable(world, this::onBlockStateChange);
        } else if (world != null) {
            BlockStateUpdateTracker.disable(world);
        }
    }

    private void onBlockStateChange(BlockPos position, BlockState before, BlockState after) {
        Zone zone = state.zone().orElse(null);
        if (!state.enabled() || zone == null || !zone.contains(dimension(),
                new fr.minlego.redstonevisualizer.core.BlockPos(
                        position.getX(), position.getY(), position.getZ()))) {
            return;
        }
        updates.recordChange(new fr.minlego.redstonevisualizer.core.BlockPos(
                position.getX(), position.getY(), position.getZ()), before, after, tick);
        rebuildAround(new fr.minlego.redstonevisualizer.core.BlockPos(
                position.getX(), position.getY(), position.getZ()));
        publish();
    }

    private void syncConfig() {
        RedstoneVisualizerConfig config = RedstoneVisualizerConfig.get();
        int nextOpacity = config.getBaseOpacity();
        int nextDuration = config.getDurationTicks();
        int nextFade = config.getFadeTicks();
        List<String> nextWhitelist = config.getWhitelist();
        if (nextOpacity == opacity && nextDuration == duration && nextFade == fade
                && nextWhitelist.equals(whitelist)) {
            return;
        }
        opacity = nextOpacity;
        duration = nextDuration;
        fade = nextFade;
        whitelist = nextWhitelist;
        updates.configure(opacity, duration, fade);
        rebuild(state.zone().orElse(null));
        publish();
    }

    private void publish() {
        TerrainMask.configure(state.zone().orElse(null), dimension(),
                isWorldAvailable() && state.enabled(), updates, Set.copyOf(whitelist), tick);
    }

    private Path stateDirectory(ClientWorld next) {
        if (next == null) {
            return null;
        }
        if (client.getServer() != null) {
            return client.getServer().getSavePath(WorldSavePath.ROOT);
        }
        ServerInfo server = client.getCurrentServerEntry();
        if (server == null && client.getNetworkHandler() != null) {
            server = client.getNetworkHandler().getServerInfo();
        }
        return server == null || server.address == null || server.address.isBlank() ? null
                : WorldStore.multiplayerDirectory(
                        FabricLoader.getInstance().getConfigDir(), server.address);
    }

    private String dimension() {
        return world == null ? null : world.getRegistryKey().getValue().toString();
    }

    private void rebuildAround(fr.minlego.redstonevisualizer.core.BlockPos position) {
        client.worldRenderer.scheduleBlockRenders(
                position.x() - 1, position.y() - 1, position.z() - 1,
                position.x() + 1, position.y() + 1, position.z() + 1);
    }

    private void rebuild(Zone zone) {
        if (world == null || zone == null || !zone.dimension().equals(dimension())) {
            return;
        }
        for (ChunkBuilder.BuiltChunk section : client.worldRenderer.getBuiltChunks()) {
            BlockPos origin = section.getOrigin();
            if ((long) origin.getX() <= (long) zone.maxX() + 1
                    && (long) origin.getX() + 15 >= (long) zone.minX() - 1
                    && (long) origin.getY() <= (long) zone.maxY() + 1
                    && (long) origin.getY() + 15 >= (long) zone.minY() - 1
                    && (long) origin.getZ() <= (long) zone.maxZ() + 1
                    && (long) origin.getZ() + 15 >= (long) zone.minZ() - 1) {
                section.scheduleRebuild(false);
            }
        }
    }

    private void save() {
        if (stateDirectory == null) {
            return;
        }
        try {
            WorldStore.save(stateDirectory, state);
        } catch (IOException exception) {
            LOGGER.warn("Could not save world visualizer settings", exception);
        }
    }
}
