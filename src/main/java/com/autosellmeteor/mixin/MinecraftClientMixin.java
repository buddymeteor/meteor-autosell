package com.autosellmeteor.mixin;

/**
 * Mixin for MinecraftClient - handles disconnect events.
 *
 * Stops auto sell and auto return when disconnecting from a server.
 *
 * @author Meteor Team
 * @license MIT
 */
import com.autosellmeteor.AutoSellMeteor;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "disconnect", at = @At("HEAD"))
    private void onDisconnect(CallbackInfo ci) {
        try {
            AutoSellMeteor mod = AutoSellMeteor.getInstance();
            if (mod == null || mod.getConfig() == null) return;
            mod.getAutoSell().stop();
            mod.getAutoReturn().stop();
        } catch (Exception e) {
            AutoSellMeteor.LOGGER.error("Error in disconnect mixin", e);
        }
    }

    @Inject(method = "run", at = @At("HEAD"))
    private void onRun(CallbackInfo ci) {
        AutoSellMeteor.LOGGER.info("Meteor AutoSell: Game starting...");
    }
}
