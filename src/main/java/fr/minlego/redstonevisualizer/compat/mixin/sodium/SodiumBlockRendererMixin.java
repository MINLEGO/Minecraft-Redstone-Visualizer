package fr.minlego.redstonevisualizer.compat.mixin.sodium;

import fr.minlego.redstonevisualizer.render.TerrainMask;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the visualizer opacity while Sodium writes a block quad. */
@Restriction(require = @Condition("sodium"))
@Mixin(BlockRenderer.class)
public abstract class SodiumBlockRendererMixin {
    @Unique
    private BlockPos redstoneVisualizer$position;

    @Unique
    private BlockState redstoneVisualizer$state;

    @Inject(method = "renderModel", at = @At("HEAD"), require = 0)
    private void redstoneVisualizer$prepareMask(BlockStateModel model, BlockState blockState,
            BlockPos blockPos, BlockPos origin, CallbackInfo callback) {
        redstoneVisualizer$position = blockPos;
        redstoneVisualizer$state = blockState;
    }

    @Inject(method = "processQuad", at = @At("HEAD"), cancellable = true, require = 0)
    private void redstoneVisualizer$maskQuad(MutableQuadViewImpl quad, CallbackInfo callback) {
        int opacity = TerrainMask.opacityAt(redstoneVisualizer$position,
                redstoneVisualizer$state);
        if (opacity == 0) {
            callback.cancel();
            return;
        }
        if (opacity == 255) {
            return;
        }

        quad.setRenderType(BlockRenderLayer.TRANSLUCENT);
        for (int vertex = 0; vertex < 4; vertex++) {
            int color = quad.baseColor(vertex);
            int alpha = ((color >>> 24) & 255) * opacity;
            quad.setColor(vertex, (color & 0x00ffffff) | (((alpha + 127) / 255) << 24));
        }
    }
}
