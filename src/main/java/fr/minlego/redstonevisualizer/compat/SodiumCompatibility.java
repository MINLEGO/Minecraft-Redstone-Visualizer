package fr.minlego.redstonevisualizer.compat;

import net.fabricmc.loader.api.FabricLoader;

/** Detects Sodium without loading any Sodium classes. */
public final class SodiumCompatibility {
    public static final String MOD_ID = "sodium";
    public static final String SUPPORTED_VERSION = "0.8.7+mc1.21.11";

    private static boolean forcedForSession;

    private SodiumCompatibility() {
    }

    public enum State {
        ABSENT,
        SUPPORTED,
        UNSUPPORTED,
        FORCED
    }

    public static State state() {
        if (!isPresent()) {
            return State.ABSENT;
        }
        if (forcedForSession) {
            return State.FORCED;
        }
        return isSupportedVersion(detectedVersion()) ? State.SUPPORTED : State.UNSUPPORTED;
    }

    /** Returns the Fabric metadata version, or {@code unknown} if metadata is unavailable. */
    public static String detectedVersion() {
        return FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    public static boolean isPresent() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static boolean isSupportedVersion(String version) {
        return SUPPORTED_VERSION.equals(version);
    }

    public static boolean isVisualizationAllowed() {
        return state() != State.UNSUPPORTED;
    }

    public static boolean requiresWarning() {
        State state = state();
        return state == State.UNSUPPORTED || state == State.FORCED;
    }

    public static boolean isForced() {
        return forcedForSession;
    }

    /** Enables the untested adapter only for this client process. */
    public static void forceForSession() {
        if (isPresent() && state() == State.UNSUPPORTED) {
            forcedForSession = true;
        }
    }
}
