package fr.minlego.redstonevisualizer.render.mixin;

import fr.minlego.redstonevisualizer.render.TerrainMask;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Indigo must draw faces beside blocks hidden by the mask. */
@Mixin(BlockRenderInfo.class)
public abstract class IndigoFaceMixin {
    @Shadow public BlockRenderView blockView;
    @Shadow public BlockPos blockPos;

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private void redstoneVisualizer$showFaceBesideMaskedBlock(Direction side,
            CallbackInfoReturnable<Boolean> result) {
        if (side == null || blockView == null || blockPos == null) {
            return;
        }
        BlockPos neighbor = blockPos.offset(side);
        if (TerrainMask.mayMask(neighbor)
                && TerrainMask.opacityAt(neighbor, blockView.getBlockState(neighbor)) < 255) {
            result.setReturnValue(true);
        }
    }
}
