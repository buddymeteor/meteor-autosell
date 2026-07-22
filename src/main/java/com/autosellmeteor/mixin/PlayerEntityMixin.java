package com.autosellmeteor.mixin;

/**
 * Mixin for PlayerEntity - enables the mod on first tick.
 *
 * @author Meteor Team
 * @license MIT
 */
import com.autosellmeteor.AutoSellMeteor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class PlayerEntityMixin {

    /** Enable the mod on the first player tick. */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        try {
            AutoSellMeteor mod = AutoSellMeteor.getInstance();
            if (mod == null || mod.isEnabled()) return;
            mod.setEnabled(true);
        } catch (Exception e) {
            AutoSellMeteor.LOGGER.error("Error in tick mixin", e);
        }
    }
}
