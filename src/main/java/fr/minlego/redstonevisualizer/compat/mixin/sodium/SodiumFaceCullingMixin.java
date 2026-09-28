package fr.minlego.redstonevisualizer.compat.mixin.sodium;

import fr.minlego.redstonevisualizer.render.TerrainMask;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps a model face visible beside a masked Sodium block. */
@Restriction(require = @Condition("sodium"))
@Mixin(AbstractBlockRenderContext.class)
public abstract class SodiumFaceCullingMixin {
    @Shadow
    protected BlockRenderView level;

    @Shadow
    protected BlockState state;

    @Shadow
    protected BlockPos pos;

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true, require = 0)
    private void redstoneVisualizer$showFaceBesideMask(Direction side,
            CallbackInfoReturnable<Boolean> callback) {
        if (side == null || level == null || pos == null) {
            return;
        }
        BlockPos neighbor = pos.offset(side);
        if (TerrainMask.mayMask(neighbor)
                && TerrainMask.opacityAt(neighbor, level.getBlockState(neighbor)) < 255) {
            callback.setReturnValue(true);
        }
    }
}
