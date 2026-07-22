package com.autosellmeteor.features;

/**
 * Detects admin players by star prefix or custom name list.
 *
 * Checks the player list for:
 * 1. Players with star/special character prefix (★ ☆ ✦ ✶ ⭐)
 * 2. Players matching custom admin names in config
 *
 * When detected, auto disconnects after a warning delay.
 *
 * @author Meteor Team
 * @license MIT
 */
import com.autosellmeteor.AutoSellMeteor;
import com.autosellmeteor.config.ModConfig;
import com.autosellmeteor.discord.DiscordWebhook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

public class AdminDetector {
    private final ModConfig config;
    private final DiscordWebhook discordWebhook;
    private boolean detected = false;
    private String lastDetectedAdmin = null;

    public AdminDetector(ModConfig config, DiscordWebhook discordWebhook) {
        this.config = config;
        this.discordWebhook = discordWebhook;
    }

    /** Scan player list for admins each tick. */
    public void tick(net.minecraft.client.world.ClientWorld world) {
        try {
            if (!config.adminDetectionEnabled || detected) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.getNetworkHandler() == null) return;

            for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
                String playerName = entry.getProfile().getName();
                if (playerName.equals(client.player.getName().getString())) continue;

                if (hasStarPrefix(playerName) || isAdminByName(playerName)) {
                    detected = true;
                    lastDetectedAdmin = playerName;
                    handleAdminDetected(client.player, playerName);
                    break;
                }
            }
        } catch (Exception e) {
            AutoSellMeteor.LOGGER.error("Error in admin detector", e);
        }
    }

    /** Check if name matches any custom admin name in config. */
    private boolean isAdminByName(String name) {
        if (config.adminNames == null) return false;
        String lower = name.toLowerCase();
        for (String admin : config.adminNames) {
            if (admin != null && lower.equals(admin.trim().toLowerCase())) return true;
        }
        return false;
    }

    /** Check if name starts with a star/special character. */
    private boolean hasStarPrefix(String name) {
        if (name.isEmpty()) return false;
        char first = name.charAt(0);
        return first == '\u2605' || first == '\u2606' || first == '\u2726' || first == '\u2727'
                || first == '\u2B50' || first == '\u2736' || first == '\u2738';
    }

    /** Handle admin detection - notify, send Discord alert, optionally disconnect. */
    private void handleAdminDetected(ClientPlayerEntity player, String adminName) {
        try {
            player.sendMessage(Text.literal("[Meteor AutoSell] Admin detected: " + adminName + "!"), false);

            if (config.discordTrackingEnabled) {
                discordWebhook.sendAdminAlert(adminName);
            }

            if (config.disconnectOnAdmin) {
                MinecraftClient client = MinecraftClient.getInstance();
                new Thread(() -> {
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                    client.execute(() -> {
                        try {
                            player.sendMessage(Text.literal("[Meteor AutoSell] Disconnecting..."), false);
                            client.disconnect();
                        } catch (Exception e) {
                            AutoSellMeteor.LOGGER.error("Error disconnecting", e);
                        }
                    });
                }).start();
            }
        } catch (Exception e) {
            AutoSellMeteor.LOGGER.error("Error handling admin detection", e);
        }
    }

    public void reset() { detected = false; lastDetectedAdmin = null; }
    public void onJoin() { detected = false; lastDetectedAdmin = null; }
    public boolean hasDetected() { return detected; }
    public String getLastDetectedAdmin() { return lastDetectedAdmin; }
}
