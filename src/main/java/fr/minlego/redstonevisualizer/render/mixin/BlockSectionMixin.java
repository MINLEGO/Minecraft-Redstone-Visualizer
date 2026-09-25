package fr.minlego.redstonevisualizer.render.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import fr.minlego.redstonevisualizer.render.AlphaVertexConsumer;
import fr.minlego.redstonevisualizer.render.TerrainMask;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.chunk.SectionBuilder;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Moves masked models out of the opaque section mesh and gives them vertex alpha. */
@Mixin(SectionBuilder.class)
public abstract class BlockSectionMixin {
    @WrapOperation(method = "build", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/chunk/ChunkOcclusionDataBuilder;markClosed(Lnet/minecraft/util/math/BlockPos;)V"))
    private void redstoneVisualizer$openOcclusion(ChunkOcclusionDataBuilder builder,
            BlockPos position, Operation<Void> original,
            @Local(index = 15) BlockState state) {
        if (TerrainMask.opacityAt(position, state) == 255) {
            original.call(builder, position);
        }
    }

    @WrapOperation(method = "build", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/BlockRenderLayers;getBlockLayer(Lnet/minecraft/block/BlockState;)Lnet/minecraft/client/render/BlockRenderLayer;"))
    private BlockRenderLayer redstoneVisualizer$layer(BlockState state,
            Operation<BlockRenderLayer> original, @Local(index = 14) BlockPos position) {
        return TerrainMask.opacityAt(position, state) < 255
                ? BlockRenderLayer.TRANSLUCENT : original.call(state);
    }

    @WrapOperation(method = "build", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/block/BlockRenderManager;renderBlock(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLjava/util/List;)V"))
    private void redstoneVisualizer$renderBlock(BlockRenderManager renderer,
            BlockState state, BlockPos position, BlockRenderView world,
            MatrixStack matrices, VertexConsumer vertices, boolean cull,
            List<?> parts, Operation<Void> original) {
        int opacity = TerrainMask.opacityAt(position, state);
        if (opacity == 0) {
            return;
        }
        original.call(renderer, state, position, world, matrices,
                opacity == 255 ? vertices : new AlphaVertexConsumer(vertices, opacity),
                cull && opacity == 255 && !TerrainMask.isHighlighted(position, state), parts);
    }
}
