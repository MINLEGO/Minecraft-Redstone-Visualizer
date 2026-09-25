package fr.minlego.redstonevisualizer;

import fr.minlego.redstonevisualizer.config.RedstoneVisualizerConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public final class RedstoneVisualizerClient implements ClientModInitializer {
    public static VisualizerSession session;

    @Override
    public void onInitializeClient() {
        session = new VisualizerSession(MinecraftClient.getInstance());
        RedstoneVisualizerConfig.register(session::toggle);
        RedstoneVisualizerConfig.get().load();
        ClientTickEvents.END_CLIENT_TICK.register(client -> session.onClientTick());
    }
}
