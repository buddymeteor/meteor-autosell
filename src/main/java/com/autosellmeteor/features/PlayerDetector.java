package com.autosellmeteor.features;

/**
 * Detects nearby players and disconnects to avoid detection.
 *
 * Scans every 10 ticks (0.5s) within a configurable radius (default 128 blocks = 8 chunks).
 * Only active when auto sell is enabled. Skips whitelisted and dead players.
 *
 * When a player is detected:
 * 1. Stops auto sell
 * 2. Turns off auto sell config
 * 3. Sends disconnect message with player name
 *
 * @author Meteor Team
 * @license MIT
 */
import com.autosellmeteor.AutoSellMeteor;
import com.autosellmeteor.config.ModConfig;
import com.autosellmeteor.discord.DiscordWebhook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class PlayerDetector {
    private final ModConfig config;
    private final WhitelistManager whitelistManager;
    private final DiscordWebhook discordWebhook;

    private int scanTickCounter = 0;
    private static final int SCAN_INTERVAL = 10;
    private boolean detected = false;
    private String detectedPlayer = null;

    public PlayerDetector(ModConfig config, WhitelistManager whitelistManager, DiscordWebhook discordWebhook) {
        this.config = config;
        this.whitelistManager = whitelistManager;
        this.discordWebhook = discordWebhook;
    }

    /** Scan for nearby players every SCAN_INTERVAL ticks. */
    public void tick(net.minecraft.client.world.ClientWorld world, ClientPlayerEntity player, boolean autoSellEnabled) {
        try {
            if (!config.playerDetectionEnabled) return;
            if (!autoSellEnabled) return;
            if (player == null || world == null) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen != null) return;

            scanTickCounter++;
            if (scanTickCounter < SCAN_INTERVAL) return;
            scanTickCounter = 0;

            // Scan all players in the world
            for (PlayerEntity entity : world.getPlayers()) {
                if (entity == player) continue;
                if (entity.getUuid().equals(player.getUuid())) continue;
                if (entity.isDead()) continue;

                String entityName = entity.getName().getString();
                if (entityName == null || entityName.isEmpty()) continue;
                if (whitelistManager.isWhitelisted(entityName)) continue;

                double distance = player.getPos().distanceTo(entity.getPos());
                if (distance > config.detectionRadius) continue;

                // Player detected in range
                detected = true;
                detectedPlayer = entityName;
                handlePlayerDetected(client, player, entityName, distance);
                break;
            }
        } catch (Exception e) {
            AutoSellMeteor.LOGGER.error("Error in player detector", e);
        }
    }

    /** Handle player detection - stop auto sell and disconnect. */
    private void handlePlayerDetected(MinecraftClient client, ClientPlayerEntity player, String playerName, double distance) {
        try {
            // Stop auto sell
            AutoSellMeteor mod = AutoSellMeteor.getInstance();
            if (mod != null) {
                mod.getAutoSell().stop();
            }
            config.autoSellEnabled = false;
            config.save();

            player.sendMessage(Text.literal(
                    "[Meteor AutoSell] Player detected: " + playerName +
                    " (" + String.format("%.1f", distance) + " blocks)"
            ), false);

            if (config.discordTrackingEnabled) {
                discordWebhook.sendPlayerAlert(playerName, distance);
            }

            // Disconnect after delay
            String disconnectMsg = "Player nearby: " + playerName;
            new Thread(() -> {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                client.execute(() -> {
                    try {
                        if (client.player != null && client.player.networkHandler != null) {
                            client.player.networkHandler.getConnection().disconnect(
                                    Text.literal(disconnectMsg)
                            );
                        }
                    } catch (Exception e) {
                        AutoSellMeteor.LOGGER.error("Error sending disconnect", e);
                    }
                });
            }).start();
        } catch (Exception e) {
            AutoSellMeteor.LOGGER.error("Error handling player detection", e);
        }
    }

    public void reset() { detected = false; detectedPlayer = null; scanTickCounter = 0; }
    public void onJoin() { detected = false; detectedPlayer = null; scanTickCounter = 0; }
    public boolean hasDetected() { return detected; }
    public String getDetectedPlayer() { return detectedPlayer; }
}
