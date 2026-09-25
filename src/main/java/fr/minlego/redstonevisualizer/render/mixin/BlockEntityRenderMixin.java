package fr.minlego.redstonevisualizer.render.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fr.minlego.redstonevisualizer.render.TerrainMask;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Prevents a fully hidden block entity from submitting its separate model. */
@Mixin(WorldRenderer.class)
public abstract class BlockEntityRenderMixin {
    @WrapOperation(method = "renderBlockEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/block/entity/BlockEntityRenderManager;render(Lnet/minecraft/client/render/block/entity/state/BlockEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V"))
    private void redstoneVisualizer$skipHiddenEntity(BlockEntityRenderManager renderer,
            BlockEntityRenderState state, MatrixStack matrices,
            OrderedRenderCommandQueue queue, CameraRenderState camera,
            Operation<Void> original) {
        if (TerrainMask.opacityAt(state.pos, state.blockState) != 0) {
            original.call(renderer, state, matrices, queue, camera);
        }
    }
}
