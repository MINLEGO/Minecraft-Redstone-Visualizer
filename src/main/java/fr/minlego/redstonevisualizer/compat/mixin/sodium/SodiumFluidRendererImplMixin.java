package fr.minlego.redstonevisualizer.compat.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import fr.minlego.redstonevisualizer.render.TerrainMask;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.fabric.render.FluidRendererImpl;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Selects Sodium's translucent buffer before partially masked fluid vertices are emitted. */
@Restriction(require = @Condition("sodium"))
@Mixin(FluidRendererImpl.class)
public abstract class SodiumFluidRendererImplMixin {
    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/DefaultMaterials;"
                    + "forFluidState(Lnet/minecraft/fluid/FluidState;)"
                    + "Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;"),
            require = 0)
    private Material redstoneVisualizer$fluidMaterial(Material original,
            @Local(argsOnly = true) BlockState blockState,
            @Local(argsOnly = true, ordinal = 0) BlockPos blockPos) {
        int opacity = TerrainMask.opacityAt(blockPos, blockState);
        return opacity > 0 && opacity < 255 ? DefaultMaterials.TRANSLUCENT : original;
    }
}
