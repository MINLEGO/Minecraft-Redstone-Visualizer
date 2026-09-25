package fr.minlego.redstonevisualizer.render.mixin;

import net.minecraft.block.BlockState;
import fr.minlego.redstonevisualizer.render.TerrainMask;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Exposes the face behind a transparent block at the edge of the selection. */
@Mixin(BlockModelRenderer.class)
public abstract class NeighborFaceMixin {
    @Inject(method = "shouldDrawFace", at = @At("HEAD"), cancellable = true)
    private static void redstoneVisualizer$exposeBehindMask(BlockRenderView world,
            BlockState state, boolean cull, Direction side, BlockPos position,
            CallbackInfoReturnable<Boolean> result) {
        if (!cull) {
            return;
        }
        BlockPos neighbor = position.offset(side);
        if (TerrainMask.mayMask(neighbor)
                && TerrainMask.opacityAt(neighbor, world.getBlockState(neighbor)) < 255) {
            result.setReturnValue(true);
        }
    }
}
