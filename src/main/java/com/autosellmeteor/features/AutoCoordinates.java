package com.autosellmeteor.features;

/**
 * Tracks player position and pauses auto sell when player moves out of range.
 *
 * When auto sell starts, the current position is saved. If the player moves
 * more than the configured threshold (default 1 block), auto sell is paused
 * to prevent spamming the sell GUI.
 *
 * @author Meteor Team
 * @license MIT
 */
import com.autosellmeteor.AutoSellMeteor;
import com.autosellmeteor.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class AutoCoordinates {
    private static Vec3d savedPosition = null;
    private static boolean wasOutOfRange = false;

    /** Save the current player position as the sell anchor point. */
    public static void savePosition() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            savedPosition = client.player.getPos();
        }
    }

    /** Clear the saved position (called when auto sell stops). */
    public static void clearPosition() {
        savedPosition = null;
        wasOutOfRange = false;
    }

    public static Vec3d getSavedPosition() { return savedPosition; }
    public static boolean isDisplaced() { return wasOutOfRange; }

    /** Get current distance from saved position. */
    public static double getCurrentDistance() {
        if (savedPosition == null) return 0;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return 0;
        return client.player.getPos().distanceTo(savedPosition);
    }

    /**
     * Check if player is still in range and pause/resume auto sell accordingly.
     * Called every tick when auto sell is active.
     */
    public static void onTick(ModConfig config, AutoSell autoSell) {
        try {
            if (!config.autoSellEnabled) return;
            if (savedPosition == null) return;
            if (!autoSell.isRunning()) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            double distance = client.player.getPos().distanceTo(savedPosition);

            if (distance > config.positionThreshold) {
                // Player moved too far - pause auto sell
                if (!wasOutOfRange) {
                    wasOutOfRange = true;
                    autoSell.pause();
                    client.player.sendMessage(Text.literal(
                            "[Meteor AutoSell] Out of range (" + String.format("%.1f", distance) + " blocks). Auto sell paused."
                    ), false);
                }
            } else {
                // Player returned to position - resume auto sell
                if (wasOutOfRange) {
                    wasOutOfRange = false;
                    autoSell.resume();
                    client.player.sendMessage(Text.literal(
                            "[Meteor AutoSell] Back in position. Resuming auto sell..."
                    ), false);
                }
            }
        } catch (Exception e) {
            AutoSellMeteor.LOGGER.error("Error in auto coordinates tick", e);
        }
    }
}
