package com.autosellmeteor.mixin;

/**
 * Mixin for ClientPlayNetworkHandler - handles chat messages for money parsing
 * and game join events for state reset.
 *
 * @author Meteor Team
 * @license MIT
 */
import com.autosellmeteor.AutoSellMeteor;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    /** Reset mod state when joining a server. */
    @Inject(method = "onGameJoin", at = @At("HEAD"))
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        try {
            AutoSellMeteor mod = AutoSellMeteor.getInstance();
            if (mod == null) return;

            mod.getPlayerDetector().onJoin();
            mod.getSellTracker().resetSession();
            mod.getAdminDetector().reset();

            AutoSellMeteor.LOGGER.info("Meteor AutoSell: Joined server, resetting state");
        } catch (Exception e) {
            AutoSellMeteor.LOGGER.error("Error on game join", e);
        }
    }

    /** Parse money amounts from chat messages for the sell tracker. */
    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        try {
            AutoSellMeteor mod = AutoSellMeteor.getInstance();
            if (mod == null || mod.getSellTracker() == null) return;
            if (mod.getConfig() == null || !mod.getConfig().autoSellEnabled) return;

            Text message = packet.content();
            mod.getSellTracker().parseMoneyFromChat(message.getString());
        } catch (Exception e) {
            AutoSellMeteor.LOGGER.error("Error parsing chat message", e);
        }
    }
}
