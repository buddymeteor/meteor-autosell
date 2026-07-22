package com.autosellmeteor.discord;

/**
 * Discord webhook integration for status updates and alerts.
 *
 * Sends embed messages to a Discord webhook for:
 * - Periodic status updates (every 60s by default)
 * - Admin detection alerts
 * - Player detection alerts
 * - Sell notifications
 *
 * All network calls are made on background threads to avoid blocking the game.
 *
 * @author Meteor Team
 * @license MIT
 */
import com.autosellmeteor.AutoSellMeteor;
import com.autosellmeteor.config.ModConfig;
import com.autosellmeteor.features.SellTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

public class DiscordWebhook {
    private final ModConfig config;
    private final HttpClient httpClient;
    private long lastUpdateTime = 0;

    public DiscordWebhook(ModConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
    }

    /** Send periodic status update if interval has elapsed. */
    public void tick(ClientPlayerEntity player, SellTracker tracker) {
        try {
            if (!config.discordTrackingEnabled) return;
            if (config.discordWebhookUrl == null || config.discordWebhookUrl.isEmpty()) return;

            long currentTime = Instant.now().getEpochSecond();
            if (currentTime - lastUpdateTime < config.discordUpdateInterval) return;

            lastUpdateTime = currentTime;
            sendStatusUpdate(player, tracker);
        } catch (Exception e) {
            AutoSellMeteor.LOGGER.error("Error in discord tick", e);
        }
    }

    /** Send a status embed with current session stats. */
    public void sendStatusUpdate(ClientPlayerEntity player, SellTracker tracker) {
        try {
            if (config.discordWebhookUrl == null || config.discordWebhookUrl.isEmpty()) return;

            MinecraftClient client = MinecraftClient.getInstance();
            String serverInfo = client.getCurrentServerEntry() != null ?
                    client.getCurrentServerEntry().address : "Singleplayer";

            String json = "{" +
                    "\"embeds\":[{\"title\":\"Meteor AutoSell Status\",\"color\":16750848," +
                    "\"fields\":[" +
                    "{\"name\":\"Server\",\"value\":\"" + serverInfo + "\",\"inline\":true}," +
                    "{\"name\":\"Position\",\"value\":\"" + String.format("%.1f, %.1f, %.1f", player.getX(), player.getY(), player.getZ()) + "\",\"inline\":true}," +
                    "{\"name\":\"Session Time\",\"value\":\"" + tracker.getSessionTimeFormatted() + "\",\"inline\":true}," +
                    "{\"name\":\"Sold\",\"value\":\"" + tracker.getSessionSold() + "\",\"inline\":true}," +
                    "{\"name\":\"Money\",\"value\":\"" + tracker.formatCurrency(tracker.getSessionMoney()) + "\",\"inline\":true}," +
                    "{\"name\":\"$/Hour\",\"value\":\"" + tracker.formatCurrency(tracker.getMoneyPerHour()) + "\",\"inline\":true}," +
                    "{\"name\":\"Total Sold\",\"value\":\"" + tracker.getTotalSold() + "\",\"inline\":true}," +
                    "{\"name\":\"Total Money\",\"value\":\"" + tracker.formatCurrency(tracker.getTotalMoney()) + "\",\"inline\":true}" +
                    "],\"footer\":{\"text\":\"Meteor AutoSell | " + Instant.now().toString() + "\"}}]}";

            sendWebhook(json);
        } catch (Exception e) {
            AutoSellMeteor.LOGGER.error("Error sending status update", e);
        }
    }

    /** Send an alert when items are sold. */
    public void sendSellAlert(int itemsSold, int totalSold) {
        try {
            if (config.discordWebhookUrl == null || config.discordWebhookUrl.isEmpty()) return;

            String json = "{" +
                    "\"embeds\":[{\"title\":\"Items Sold!\",\"color\":3066993," +
                    "\"description\":\"Sold **" + itemsSold + "** items this round! Total: **" + totalSold + "**\"," +
                    "\"footer\":{\"text\":\"Meteor AutoSell | " + Instant.now().toString() + "\"}}]}";

            sendWebhook(json);
        } catch (Exception e) {
            AutoSellMeteor.LOGGER.error("Error sending sell alert", e);
        }
    }

    /** Send an alert when an admin is detected. */
    public void sendAdminAlert(String adminName) {
        try {
            if (config.discordWebhookUrl == null || config.discordWebhookUrl.isEmpty() || !config.discordNotifyOnAdmin) return;

            String json = "{" +
                    "\"embeds\":[{\"title\":\"Admin Detected!\",\"color\":15158332," +
                    "\"description\":\"Admin **" + adminName + "** has been detected. Disconnecting...\"," +
                    "\"footer\":{\"text\":\"Meteor AutoSell | " + Instant.now().toString() + "\"}}]}";

            sendWebhook(json);
        } catch (Exception e) {
            AutoSellMeteor.LOGGER.error("Error sending admin alert", e);
        }
    }

    /** Send an alert when a player is detected nearby. */
    public void sendPlayerAlert(String playerName, double distance) {
        try {
            if (config.discordWebhookUrl == null || config.discordWebhookUrl.isEmpty() || !config.discordNotifyOnPlayer) return;

            String json = "{" +
                    "\"embeds\":[{\"title\":\"Player Detected!\",\"color\":15105570," +
                    "\"description\":\"Player **" + playerName + "** detected at " + String.format("%.1f", distance) + " blocks. Disconnecting...\"," +
                    "\"footer\":{\"text\":\"Meteor AutoSell | " + Instant.now().toString() + "\"}}]}";

            sendWebhook(json);
        } catch (Exception e) {
            AutoSellMeteor.LOGGER.error("Error sending player alert", e);
        }
    }

    /** Send a webhook request on a background thread. */
    private void sendWebhook(String json) {
        if (config.discordWebhookUrl == null || config.discordWebhookUrl.isEmpty()) return;

        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(config.discordWebhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                AutoSellMeteor.LOGGER.error("Error sending webhook", e);
            }
        }).start();
    }
}
