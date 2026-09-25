package fr.minlego.redstonevisualizer.render.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fr.minlego.redstonevisualizer.render.AlphaVertexConsumer;
import fr.minlego.redstonevisualizer.render.TerrainMask;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.chunk.SectionBuilder;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SectionBuilder.class)
public abstract class FluidSectionMixin {
    @WrapOperation(
            method = "build",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/RenderLayers;getFluidLayer(Lnet/minecraft/fluid/FluidState;)Lnet/minecraft/client/render/BlockRenderLayer;"))
    private BlockRenderLayer redstoneVisualizer$fluidLayer(
            FluidState fluidState,
            Operation<BlockRenderLayer> original,
            @Local(ordinal = 2) BlockPos position,
            @Local(index = 15) BlockState blockState) {
        int opacity = TerrainMask.opacityAt(position, blockState);
        return opacity < 255 ? BlockRenderLayer.TRANSLUCENT : original.call(fluidState);
    }

    @WrapOperation(
            method = "build",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/block/BlockRenderManager;renderFluid(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/block/BlockState;Lnet/minecraft/fluid/FluidState;)V"))
    private void redstoneVisualizer$fluidVertices(
            BlockRenderManager renderer,
            BlockPos position,
            BlockRenderView world,
            VertexConsumer vertexConsumer,
            BlockState blockState,
            FluidState fluidState,
            Operation<Void> original) {
        int opacity = TerrainMask.opacityAt(position, blockState);
        if (opacity == 0) {
            return;
        }
        if (opacity == 255) {
            original.call(renderer, position, world, vertexConsumer, blockState, fluidState);
            return;
        }
        original.call(renderer, position, world,
                new AlphaVertexConsumer(vertexConsumer, opacity), blockState, fluidState);
    }
}
