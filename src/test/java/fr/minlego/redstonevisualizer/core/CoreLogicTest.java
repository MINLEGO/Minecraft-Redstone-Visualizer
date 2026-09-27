package fr.minlego.redstonevisualizer.core;

import fr.minlego.redstonevisualizer.compat.SodiumCompatibility;
import fr.minlego.redstonevisualizer.render.BlockEntityOpacityQueue;
import java.util.OptionalLong;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.Identifier;

/** Standalone smoke test: javac ... && java ...CoreLogicTest. */
public final class CoreLogicTest {
    private CoreLogicTest() {
    }

    public static void main(String[] args) {
        zoneIsInclusiveAndDimensionAware();
        zoneSizeDoesNotOverflowSilently();
        alphaFadesAndRelCanBeRestarted();
        whitelistAndStateChanges();
        blockEntityOpacityRoutesAndMultipliesAlpha();
        sodiumCompatibilityUsesTheExactValidatedVersion();
        System.out.println("CoreLogicTest OK");
    }

    private static void sodiumCompatibilityUsesTheExactValidatedVersion() {
        check(SodiumCompatibility.isSupportedVersion("0.8.7+mc1.21.11"),
                "validated Sodium version is accepted");
        check(!SodiumCompatibility.isSupportedVersion("0.8.8+mc1.21.11"),
                "other Sodium versions require an explicit session override");
    }

    private static void zoneIsInclusiveAndDimensionAware() {
        Zone zone = new Zone("minecraft:overworld",
                new BlockPos(3, 5, -2), new BlockPos(1, 4, 0));
        check(zone.contains(new BlockPos(1, 4, -2)), "zone includes both corners");
        check(zone.contains("minecraft:overworld", new BlockPos(2, 5, 0)),
                "zone includes its upper boundary");
        check(!zone.contains("minecraft:the_nether", new BlockPos(2, 5, 0)),
                "zone is dimension aware");
        check(zone.size() == 18, "inclusive zone size");
    }

    private static void zoneSizeDoesNotOverflowSilently() {
        Zone huge = new Zone("overworld",
                new BlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE),
                new BlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
        check(huge.sizeOverflow(), "huge zone reports overflow");
        check(huge.size() == Long.MAX_VALUE, "huge zone uses saturated size");
        check(huge.exceeds(32_768), "overflowed zone exceeds threshold");
        check(huge.exactSize().equals(OptionalLong.empty()), "exact size is unavailable");
    }

    private static void alphaFadesAndRelCanBeRestarted() {
        check(AlphaCalculator.alpha(0, 0, 12, 4) == 100, "update starts fully visible");
        check(AlphaCalculator.alpha(0, 8, 12, 4) == 100, "fade starts at its boundary");
        check(AlphaCalculator.alpha(0, 9, 12, 4) == 75, "fade tick one");
        check(AlphaCalculator.alpha(0, 11, 12, 4) == 25, "fade tick three");
        check(AlphaCalculator.alpha(0, 12, 12, 4) == 0, "expired update uses base opacity");
        check(AlphaCalculator.alpha(35, 7, 12, 0) == 100, "disabled fade holds during duration");
        check(AlphaCalculator.alpha(35, 12, 12, 0) == 35, "disabled fade returns at duration");

        AlphaTracker tracker = new AlphaTracker(0, 12, 4);
        BlockPos pos = new BlockPos(0, 64, 0);
        tracker.recordUpdate(pos, 10);
        check(tracker.alpha(pos, 18) == 100, "tracker records update");
        tracker.recordUpdate(pos, 18);
        check(tracker.alpha(pos, 26) == 100, "new update restarts window");
        check(tracker.purge(29) == 0, "active update is retained");
        check(tracker.purge(30) == 1, "expired update is purged");
        check(tracker.trackedCount() == 0, "purge removes expired update");
    }

    private static void whitelistAndStateChanges() {
        BlockWhitelist whitelist = new BlockWhitelist();
        check(BlockWhitelist.isValidIdentifier("minecraft:redstone_lamp"), "valid identifier");
        check(!BlockWhitelist.isValidIdentifier("Minecraft:Stone"), "invalid identifier case");
        check(whitelist.add("minecraft:redstone_lamp"), "whitelist add");
        check(whitelist.contains("minecraft:redstone_lamp"), "whitelist contains");

        AlphaTracker tracker = new AlphaTracker();
        BlockPos pos = new BlockPos(1, 2, 3);
        check(!tracker.recordChange(pos, "minecraft:stone", "minecraft:stone", 1),
                "same state is ignored");
        check(tracker.recordChange(pos, "minecraft:stone", "minecraft:redstone_lamp", 1),
                "changed state is recorded");
        check(tracker.alpha(pos, "minecraft:redstone_lamp", 50, whitelist) == 100,
                "whitelisted block stays visible");
    }

    private static void blockEntityOpacityRoutesAndMultipliesAlpha() {
        check(!BlockEntityOpacityQueue.shouldRender(0), "zero opacity skips block entities");
        check(BlockEntityOpacityQueue.shouldWrap(76), "intermediate opacity wraps commands");
        check(!BlockEntityOpacityQueue.shouldWrap(255), "full opacity keeps vanilla queue");
        check(BlockEntityOpacityQueue.applyOpacity(0x80ABCDEF, 128) == 0x40ABCDEF,
                "command opacity multiplies existing alpha");
        var cullPipeline = BlockEntityOpacityQueue.translucent(
                RenderLayers.entityCutout(Identifier.ofVanilla("textures/entity/chest/normal.png")))
                .getRenderPipeline();
        check(cullPipeline.isCull(), "translucent conversion preserves culling");
        check(cullPipeline.getDepthTestFunction() == DepthTestFunction.LESS_DEPTH_TEST,
                "translucent conversion rejects coplanar faces");
        var noCullPipeline = BlockEntityOpacityQueue.translucent(
                RenderLayers.entityCutoutNoCull(Identifier.ofVanilla(
                        "textures/entity/enderdragon/enderdragon.png"))).getRenderPipeline();
        check(!noCullPipeline.isCull(), "no-cull conversion preserves no-cull geometry");
        check(noCullPipeline.getDepthTestFunction() == DepthTestFunction.LESS_DEPTH_TEST,
                "no-cull conversion rejects coplanar faces");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
