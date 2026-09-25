package fr.minlego.redstonevisualizer.update.mixin;

import net.minecraft.block.BlockState;
import fr.minlego.redstonevisualizer.update.BlockStateUpdateTracker;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public abstract class ClientWorldBlockStateMixin {
    // Server packets call this method, which invokes World.setBlockState directly.
    @Inject(method = "handleBlockUpdate", at = @At("HEAD"))
    private void redstoneVisualizer$beforeServerBlockUpdate(
            BlockPos position, BlockState newState, int flags, CallbackInfo callback) {
        BlockStateUpdateTracker.begin((ClientWorld) (Object) this, position);
    }

    @Inject(method = "handleBlockUpdate", at = @At("RETURN"))
    private void redstoneVisualizer$afterServerBlockUpdate(
            BlockPos position, BlockState newState, int flags, CallbackInfo callback) {
        BlockStateUpdateTracker.finish((ClientWorld) (Object) this, true);
    }

    @Inject(
            method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z",
            at = @At("HEAD"))
    private void redstoneVisualizer$beforeBlockStateUpdate(
            BlockPos position,
            BlockState newState,
            int flags,
            int maxUpdateDepth,
            CallbackInfoReturnable<Boolean> callback) {
        BlockStateUpdateTracker.begin((ClientWorld) (Object) this, position);
    }

    @Inject(
            method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z",
            at = @At("RETURN"))
    private void redstoneVisualizer$afterBlockStateUpdate(
            BlockPos position,
            BlockState newState,
            int flags,
            int maxUpdateDepth,
            CallbackInfoReturnable<Boolean> callback) {
        BlockStateUpdateTracker.finish(
                (ClientWorld) (Object) this,
                callback.getReturnValue());
    }
}
