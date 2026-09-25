package fr.minlego.redstonevisualizer.config;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import java.util.List;

/** Minimal MaLiLib screen exposing the general visualizer settings. */
public final class RedstoneVisualizerConfigScreen extends GuiConfigsBase {
    public RedstoneVisualizerConfigScreen() {
        super(10, 50, RedstoneVisualizerConfig.MOD_ID, null,
                "redstonevisualizer.config.title");
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        return ConfigOptionWrapper.createFor(RedstoneVisualizerConfig.OPTIONS);
    }
}
