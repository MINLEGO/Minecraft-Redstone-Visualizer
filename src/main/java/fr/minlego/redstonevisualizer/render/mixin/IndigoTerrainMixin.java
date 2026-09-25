package fr.minlego.redstonevisualizer.render.mixin;

import fr.minlego.redstonevisualizer.render.AlphaVertexConsumer;
import fr.minlego.redstonevisualizer.render.TerrainMask;
import java.util.function.Function;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractTerrainRenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.TerrainRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Indigo bypasses SectionBuilder's vanilla renderBlock call for every model. */
@Mixin(TerrainRenderContext.class)
public abstract class IndigoTerrainMixin extends AbstractTerrainRenderContext {
    @Shadow
    private Function<BlockRenderLayer, BufferBuilder> bufferFunc;

    @Inject(method = "bufferModel", at = @At("HEAD"), cancellable = true)
    private void redstoneVisualizer$skipHiddenModel(BlockStateModel model, BlockState state,
            BlockPos position, CallbackInfo callback) {
        if (TerrainMask.opacityAt(position, state) == 0) {
            callback.cancel();
        }
    }

    @Inject(method = "getVertexConsumer", at = @At("HEAD"), cancellable = true)
    private void redstoneVisualizer$translucentModel(BlockRenderLayer layer,
            CallbackInfoReturnable<VertexConsumer> result) {
        int opacity = TerrainMask.opacityAt(blockInfo.blockPos, blockInfo.blockState);
        if (opacity > 0 && opacity < 255) {
            result.setReturnValue(new AlphaVertexConsumer(
                    bufferFunc.apply(BlockRenderLayer.TRANSLUCENT), opacity));
        }
    }
}
