package fr.minlego.redstonevisualizer.compat.mixin.sodium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fr.minlego.redstonevisualizer.render.TerrainMask;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Does not let masked blocks close Sodium's section visibility graph. */
@Restriction(require = @Condition("sodium"))
@Mixin(ChunkBuilderMeshingTask.class)
public abstract class SodiumSectionOcclusionMixin {
    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/chunk/ChunkOcclusionDataBuilder;markClosed(Lnet/minecraft/util/math/BlockPos;)V"),
            require = 0)
    private void redstoneVisualizer$openMaskedBlock(ChunkOcclusionDataBuilder builder,
            BlockPos position, Operation<Void> original,
            @Local(index = 23) BlockState state) {
        if (TerrainMask.opacityAt(position, state) == 255) {
            original.call(builder, position);
        }
    }
}
