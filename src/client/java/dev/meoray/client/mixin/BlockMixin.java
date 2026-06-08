package dev.meoray.client.mixin;

import dev.meoray.client.MeoRayClient;
import dev.meoray.client.core.Module;
import dev.meoray.client.feature.render.XRay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class BlockMixin {

    @Inject(method = "isSideInvisible", at = @At("HEAD"), cancellable = true)
    private void onIsSideInvisible(BlockState state, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        try {
            Module xrayMod = MeoRayClient.INSTANCE.moduleManager.getByName("XRay");
            if (xrayMod == null || !xrayMod.isEnabled() || !(xrayMod instanceof XRay xray)) return;
            if (!xray.shouldHideNonOres()) return;

            BlockState self = (BlockState)(Object) this;

            if (xray.isOre(self)) {
                cir.setReturnValue(false);
                return;
            }
            cir.setReturnValue(true);
        } catch (Throwable ignored) {}
    }
}
