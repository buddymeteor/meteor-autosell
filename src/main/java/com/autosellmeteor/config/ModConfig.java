package com.autosellmeteor.config;

/**
 * Configuration management for Meteor AutoSell.
 *
 * All settings are stored in a JSON file at config/meteor-autosell.json.
 * This file is fully readable and editable by the user.
 *
 * @author Meteor Team
 * @license MIT
 */
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("meteor-autosell.json");

    // ==================== General ====================
    public boolean modEnabled = true;
    public boolean showStatusHud = true;

    // ==================== Auto Sell ====================
    /** Whether auto sell feature is enabled. */
    public boolean autoSellEnabled = false;

    /** The sell command to send (e.g., "sell" for /sell). */
    public String sellCommand = "sell";

    /** Delay in ticks before sending the sell command after GUI opens. */
    public int sellCommandDelayTicks = 3;

    /** Delay in ticks between moving items to the sell GUI. */
    public int moveDelayTicks = 1;

    /** Delay in ticks for bulk moves when inventory is full. */
    public int bulkMoveDelayTicks = 2;

    /** Delay in ticks after clicking the confirm sell button. */
    public int sellDelayTicks = 4;

    /** Maximum number of items to move per tick. */
    public int itemsPerTick = 9;

    /** Whether to skip tool items (pickaxe, sword, etc.) when selling. */
    public boolean skipTools = true;

    /** Whether to skip armor items when selling. */
    public boolean skipArmor = true;

    /** Distance threshold in blocks before auto sell pauses. */
    public double positionThreshold = 1.0;

    // ==================== Auto Return ====================
    /** Whether auto return feature is enabled. */
    public boolean autoReturnEnabled = false;

    /** Saved return position coordinates. */
    public double returnX = 0;
    public double returnY = 0;
    public double returnZ = 0;

    /** Whether to return when pushed away from position. */
    public boolean returnOnPush = true;

    /** Whether to return after respawning from death. */
    public boolean returnOnDeath = true;

    /** Whether to return after reconnecting. */
    public boolean returnOnDisconnect = true;

    /** Distance in blocks that triggers auto return. */
    public double pushDetectionRadius = 2.0;

    /** Delay in ticks between movement steps when returning. */
    public int returnWalkDelay = 100;

    // ==================== Admin Detection ====================
    /** Whether admin detection feature is enabled. */
    public boolean adminDetectionEnabled = false;

    /** Whether to disconnect when admin is detected. */
    public boolean disconnectOnAdmin = true;

    /** List of admin names to detect (comma-separated in config). */
    public String[] adminNames = {};

    // ==================== Player Detection ====================
    /** Whether player detection feature is enabled. */
    public boolean playerDetectionEnabled = false;

    /** Whether to disconnect when unknown player is detected. */
    public boolean disconnectOnPlayer = true;

    /** Scan radius in blocks (128 = 8 chunks). */
    public double detectionRadius = 128.0;

    // ==================== Whitelist ====================
    /** List of whitelisted player names (won't trigger detection). */
    public String[] whitelistPlayers = {};

    // ==================== Discord ====================
    /** Whether Discord webhook integration is enabled. */
    public boolean discordTrackingEnabled = false;

    /** Discord webhook URL for sending notifications. */
    public String discordWebhookUrl = "";

    /** Interval in seconds between Discord status updates. */
    public int discordUpdateInterval = 60;

    /** Whether to send Discord notification when admin is detected. */
    public boolean discordNotifyOnAdmin = true;

    /** Whether to send Discord notification when player is detected. */
    public boolean discordNotifyOnPlayer = true;

    // ==================== Statistics ====================
    /** Total items sold across all sessions (persisted). */
    public int totalSold = 0;

    /** Total money earned across all sessions (persisted). */
    public double totalMoney = 0.0;

    // ==================== Save / Load ====================

    /** Save the current configuration to disk. */
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Load configuration from disk, or create default if not found. */
    public static ModConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                return GSON.fromJson(json, ModConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ModConfig config = new ModConfig();
        config.save();
        return config;
    }
}
