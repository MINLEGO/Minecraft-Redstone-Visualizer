package fr.minlego.redstonevisualizer.config;

import java.util.List;

public final class WhitelistEditorTest {
    public static void main(String[] args) {
        var config = RedstoneVisualizerConfig.get();
        var entries = RedstoneVisualizerConfig.WHITELIST.getStrings();

        entries.clear();
        entries.add("");
        RedstoneVisualizerConfig.WHITELIST.setModified();
        config.onConfigsChanged();
        check(entries.equals(List.of("")), "new editor entry must remain available for typing");
        check(config.getWhitelist().isEmpty(), "unfinished entry must not affect rendering");

        entries.set(0, "minecraft:redstone_lamp");
        RedstoneVisualizerConfig.WHITELIST.setModified();
        config.onConfigsChanged();
        check(config.getWhitelist().equals(List.of("minecraft:redstone_lamp")),
                "completed entry must become active");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
