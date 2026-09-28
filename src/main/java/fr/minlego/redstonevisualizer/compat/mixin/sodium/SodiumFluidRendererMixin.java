package fr.minlego.redstonevisualizer.compat.mixin.sodium;

import fr.minlego.redstonevisualizer.render.TerrainMask;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies the visualizer opacity while Sodium writes fluid quads. */
@Restriction(require = @Condition("sodium"))
@Mixin(DefaultFluidRenderer.class)
public abstract class SodiumFluidRendererMixin {
    @Shadow
    private int[] quadColors;

    @Unique
    private int redstoneVisualizer$opacity;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void redstoneVisualizer$prepareOpacity(LevelSlice level, BlockState blockState,
            FluidState fluidState, BlockPos blockPos, BlockPos offset,
            TranslucentGeometryCollector collector, ChunkModelBuilder meshBuilder,
            Material material, ColorProvider<FluidState> colorProvider, Sprite[] sprites,
            CallbackInfo callback) {
        redstoneVisualizer$opacity = TerrainMask.opacityAt(blockPos, blockState);
        if (redstoneVisualizer$opacity == 0) {
            callback.cancel();
        }
    }

    @Inject(method = "updateQuad", at = @At("RETURN"), require = 0)
    private void redstoneVisualizer$maskQuad(CallbackInfo callback) {
        if (redstoneVisualizer$opacity == 255) {
            return;
        }
        for (int vertex = 0; vertex < 4; vertex++) {
            int color = quadColors[vertex];
            int alpha = ((color >>> 24) & 255) * redstoneVisualizer$opacity;
            quadColors[vertex] = (color & 0x00ffffff) | (((alpha + 127) / 255) << 24);
        }
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/DefaultFluidRenderer;"
                    + "writeQuad(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;"
                    + "Lnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/TranslucentGeometryCollector;"
                    + "Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;"
                    + "Lnet/minecraft/util/math/BlockPos;"
                    + "Lnet/caffeinemc/mods/sodium/client/model/quad/ModelQuadView;"
                    + "Lnet/caffeinemc/mods/sodium/client/model/quad/properties/ModelQuadFacing;Z)V"),
            index = 2, require = 0)
    private Material redstoneVisualizer$fluidMaterial(Material material) {
        return redstoneVisualizer$opacity > 0 && redstoneVisualizer$opacity < 255
                ? DefaultMaterials.TRANSLUCENT : material;
    }

    @Inject(method = "isFullBlockFluidSideVisible", at = @At("HEAD"),
            cancellable = true, require = 0)
    private void redstoneVisualizer$showMaskedFluidFaces(BlockView view, BlockPos position,
            Direction side, FluidState fluidState, CallbackInfoReturnable<Boolean> callback) {
        BlockPos neighbor = position.offset(side);
        if (isMasked(view, position) || isMasked(view, neighbor)) {
            callback.setReturnValue(true);
        }
    }

    @Unique
    private static boolean isMasked(BlockView view, BlockPos position) {
        return TerrainMask.mayMask(position)
                && TerrainMask.opacityAt(position, view.getBlockState(position)) < 255;
    }
}
