package fr.minlego.redstonevisualizer.render.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fr.minlego.redstonevisualizer.render.TerrainMask;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps faces of a transparent fluid even where the adjacent fluid matches. */
@Mixin(FluidRenderer.class)
public abstract class FluidFaceMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/block/FluidRenderer;shouldRenderSide(Lnet/minecraft/fluid/FluidState;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;Lnet/minecraft/fluid/FluidState;)Z"))
    private boolean redstoneVisualizer$showFluidFace(FluidState fluid, BlockState state,
            Direction side, FluidState neighbor, Operation<Boolean> original,
            @Local(argsOnly = true, index = 2) BlockPos position) {
        return (TerrainMask.mayMask(position)
                && TerrainMask.opacityAt(position, state) < 255)
                || original.call(fluid, state, side, neighbor);
    }
}
