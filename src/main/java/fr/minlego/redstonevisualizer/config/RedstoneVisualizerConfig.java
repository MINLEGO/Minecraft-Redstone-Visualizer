package fr.minlego.redstonevisualizer.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fr.minlego.redstonevisualizer.core.BlockWhitelist;
import fr.minlego.redstonevisualizer.ui.WorldSelectionScreen;
import fi.dy.masa.malilib.gui.GuiBase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** MaLiLib-backed general settings for the client visualizer. */
public final class RedstoneVisualizerConfig implements IConfigHandler {
    public static final String MOD_ID = "redstone_visualizer";
    private static final String CONFIG_FILE_NAME = MOD_ID + ".json";

    public static final int DEFAULT_BASE_OPACITY = 0;
    public static final int DEFAULT_DURATION_TICKS = 12;
    public static final int DEFAULT_FADE_TICKS = 4;
    public static final int DEFAULT_ALERT_THRESHOLD = 32_768;

    public static final ConfigInteger BASE_OPACITY = new ConfigInteger(
            "baseOpacity", DEFAULT_BASE_OPACITY, 0, 100,
            "redstonevisualizer.config.comment.baseOpacity")
            .translatedName("redstonevisualizer.config.name.baseOpacity");
    public static final ConfigInteger DURATION_TICKS = new ConfigInteger(
            "durationTicks", DEFAULT_DURATION_TICKS, 1, Integer.MAX_VALUE, false,
            "redstonevisualizer.config.comment.durationTicks")
            .translatedName("redstonevisualizer.config.name.durationTicks");
    public static final ConfigInteger FADE_TICKS = new ConfigInteger(
            "fadeTicks", DEFAULT_FADE_TICKS, 0, Integer.MAX_VALUE, false,
            "redstonevisualizer.config.comment.fadeTicks")
            .translatedName("redstonevisualizer.config.name.fadeTicks");
    public static final ConfigInteger ALERT_THRESHOLD = new ConfigInteger(
            "alertThreshold", DEFAULT_ALERT_THRESHOLD, 0, Integer.MAX_VALUE, false,
            "redstonevisualizer.config.comment.alertThreshold")
            .translatedName("redstonevisualizer.config.name.alertThreshold");
    public static final ConfigStringList WHITELIST = new ConfigStringList(
            "whitelist", ImmutableList.of(),
            "redstonevisualizer.config.comment.whitelist")
            .translatedName("redstonevisualizer.config.name.whitelist");
    public static final ConfigHotkey TOGGLE = new ConfigHotkey(
            "toggle", "R",
            "redstonevisualizer.config.comment.toggle")
            .translatedName("redstonevisualizer.config.name.toggle");
    public static final ConfigHotkey OPEN_MENU = new ConfigHotkey(
            "openMenu", "V",
            "redstonevisualizer.config.comment.openMenu")
            .translatedName("redstonevisualizer.config.name.openMenu");

    public static final List<IConfigBase> OPTIONS = List.of(
            BASE_OPACITY, DURATION_TICKS, FADE_TICKS,
            ALERT_THRESHOLD, WHITELIST, TOGGLE, OPEN_MENU);

    private static final List<ConfigHotkey> HOTKEYS = List.of(TOGGLE, OPEN_MENU);
    private static final RedstoneVisualizerConfig INSTANCE = new RedstoneVisualizerConfig();
    private static final IKeybindProvider KEYBIND_PROVIDER = new IKeybindProvider() {
        @Override
        public void addKeysToMap(IKeybindManager manager) {
            manager.addKeybindToMap(TOGGLE.getKeybind());
            manager.addKeybindToMap(OPEN_MENU.getKeybind());
        }

        @Override
        public void addHotkeys(IKeybindManager manager) {
            manager.addHotkeysForCategory(MOD_ID, "redstonevisualizer.config.hotkeys", HOTKEYS);
        }
    };

    private static boolean registered;

    static {
        DURATION_TICKS.setValueChangeCallback(ignored -> normalize());
        FADE_TICKS.setValueChangeCallback(ignored -> normalize());
        OPEN_MENU.getKeybind().setCallback((action, keybind) -> {
            WorldSelectionScreen.open();
            return true;
        });
    }

    private RedstoneVisualizerConfig() {
    }

    public static RedstoneVisualizerConfig get() {
        return INSTANCE;
    }

    /** Registers the handler and keybind with the verified MaLiLib 0.26.8 APIs. */
    public static void register() {
        if (registered) {
            return;
        }
        ConfigManager.getInstance().registerConfigHandler(MOD_ID, INSTANCE);
        InputEventHandler.getKeybindManager().registerKeybindProvider(KEYBIND_PROVIDER);
        registered = true;
    }

    /** Registers the config and delegates the world-scoped ON/OFF transition. */
    public static void register(Runnable toggleAction) {
        Objects.requireNonNull(toggleAction, "toggleAction");
        register();
        TOGGLE.getKeybind().setCallback((action, keybind) -> {
            toggleAction.run();
            return true;
        });
    }

    public static void openScreen() {
        GuiBase.openGui(new RedstoneVisualizerConfigScreen());
    }

    public int getBaseOpacity() {
        return BASE_OPACITY.getIntegerValue();
    }

    public int getDurationTicks() {
        return DURATION_TICKS.getIntegerValue();
    }

    public int getFadeTicks() {
        return Math.min(FADE_TICKS.getIntegerValue(), getDurationTicks());
    }

    public int getAlertThreshold() {
        return ALERT_THRESHOLD.getIntegerValue();
    }

    public List<String> getWhitelist() {
        return List.copyOf(validWhitelist());
    }

    public ConfigHotkey getToggleHotkey() {
        return TOGGLE;
    }

    public void setBaseOpacity(int value) {
        BASE_OPACITY.setIntegerValue(value);
    }

    public void setDurationTicks(int value) {
        DURATION_TICKS.setIntegerValue(value);
        normalize();
    }

    public void setFadeTicks(int value) {
        FADE_TICKS.setIntegerValue(value);
        normalize();
    }

    public void setAlertThreshold(int value) {
        ALERT_THRESHOLD.setIntegerValue(value);
    }

    public void setWhitelist(Collection<String> values) {
        Objects.requireNonNull(values, "values");
        LinkedHashSet<String> valid = new LinkedHashSet<>();
        for (String value : values) {
            if (!BlockWhitelist.isValidIdentifier(value)) {
                throw new IllegalArgumentException("invalid block identifier: " + value);
            }
            valid.add(value);
        }
        WHITELIST.setStrings(new ArrayList<>(valid));
    }

    @Override
    public void load() {
        Path file = FileUtils.getConfigDirectoryAsPath().resolve(CONFIG_FILE_NAME);
        if (Files.isRegularFile(file) && Files.isReadable(file)) {
            JsonElement element = JsonUtils.parseJsonFileAsPath(file);
            if (element != null && element.isJsonObject()) {
                ConfigUtils.readConfigBase(element.getAsJsonObject(), "General", OPTIONS);
            }
        }
        normalize();
    }

    @Override
    public void save() {
        normalize();
        Path directory = FileUtils.getConfigDirectoryAsPath();
        try {
            Files.createDirectories(directory);
        } catch (IOException ignored) {
            return;
        }

        JsonObject root = new JsonObject();
        ConfigUtils.writeConfigBase(root, "General", OPTIONS);
        JsonUtils.writeJsonToFileAsPath(root, directory.resolve(CONFIG_FILE_NAME));
    }

    @Override
    public void onConfigsChanged() {
        normalize();
    }

    private static void normalize() {
        int duration = DURATION_TICKS.getIntegerValue();
        int fade = Math.min(FADE_TICKS.getIntegerValue(), duration);
        if (FADE_TICKS.getIntegerValue() != fade) {
            FADE_TICKS.setIntegerValue(fade);
        }
    }

    private static List<String> validWhitelist() {
        List<String> valid = new ArrayList<>();
        for (String value : WHITELIST.getStrings()) {
            if (BlockWhitelist.isValidIdentifier(value) && !valid.contains(value)) {
                valid.add(value);
            }
        }
        return valid;
    }
}
