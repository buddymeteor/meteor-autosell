package com.autosellmeteor.features;

/**
 * Auto return to saved position when pushed away or after death.
 *
 * Records the player's position when enabled, and automatically walks back
 * to that position if the player is pushed away or respawns.
 *
 * @author Meteor Team
 * @license MIT
 */
import com.autosellmeteor.AutoSellMeteor;
import com.autosellmeteor.config.ModConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class AutoReturn {
    private final ModConfig config;
    private Vec3d savedPosition = null;
    private boolean returning = false;
    private int moveTick = 0;

    public AutoReturn(ModConfig config) {
        this.config = config;
    }

    /** Record current position as the return target. */
    public void recordPosition(ClientPlayerEntity player) {
        if (player == null) return;
        savedPosition = player.getPos();
        returning = false;
        player.sendMessage(Text.literal("[Auto Return] Position saved!"), false);
    }

    /** Tick method - check if player needs to return and move towards saved position. */
    public void tick(ClientPlayerEntity player) {
        try {
            if (!config.autoReturnEnabled || savedPosition == null || player == null) return;

            if (shouldReturn(player) && !returning) {
                returning = true;
                moveTick = 0;
                player.sendMessage(Text.literal("[Auto Return] Returning to sell area..."), false);
            }

            if (returning) {
                moveToPosition(player);
            }
        } catch (Exception e) {
            AutoSellMeteor.LOGGER.error("Error in auto return tick", e);
        }
    }

    /** Check if player should return (pushed too far from saved position). */
    private boolean shouldReturn(ClientPlayerEntity player) {
        if (config.returnOnPush) {
            return player.getPos().distanceTo(savedPosition) > config.pushDetectionRadius;
        }
        return false;
    }

    /** Move the player towards the saved position. */
    private void moveToPosition(ClientPlayerEntity player) {
        try {
            moveTick++;
            if (moveTick < config.returnWalkDelay / 50) return;
            moveTick = 0;

            Vec3d currentPos = player.getPos();
            double distance = currentPos.distanceTo(savedPosition);

            if (distance < 0.5) {
                returning = false;
                player.sendMessage(Text.literal("[Auto Return] Returned to sell area!"), false);
                return;
            }

            // Calculate movement direction
            double dx = savedPosition.x - currentPos.x;
            double dz = savedPosition.z - currentPos.z;
            double speed = 0.2873; // Sprint speed

            player.setVelocity(dx / distance * speed, player.getVelocity().y, dz / distance * speed);
            player.velocityModified = true;
            player.setYaw((float) (Math.toDegrees(Math.atan2(-dx, dz))));
        } catch (Exception e) {
            AutoSellMeteor.LOGGER.error("Error moving to position", e);
        }
    }

    /** Handle death event - queue return after respawn. */
    public void onDeath(ClientPlayerEntity player) {
        if (config.returnOnDeath && savedPosition != null && player != null) {
            returning = true;
            moveTick = 0;
            player.sendMessage(Text.literal("[Auto Return] Will return after respawn!"), false);
        }
    }

    public void stop() { returning = false; savedPosition = null; }
    public boolean isReturning() { return returning; }
}
