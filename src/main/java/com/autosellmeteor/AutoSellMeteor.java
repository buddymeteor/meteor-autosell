package com.autosellmeteor;

/**
 * Meteor AutoSell - Main mod entry point.
 *
 * Open source Minecraft Fabric mod for auto selling items via /sell GUI.
 * Features: Auto Sell, Admin Detection, Player Detection, Auto Return, HUD, Discord Integration.
 *
 * @author Meteor Team
 * @version 1.0.0
 * @license MIT
 */
import com.autosellmeteor.config.ModConfig;
import com.autosellmeteor.features.*;
import com.autosellmeteor.gui.ConfigScreen;
import com.autosellmeteor.discord.DiscordWebhook;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoSellMeteor implements ClientModInitializer {
    /** Mod identifier used for logging and resource paths. */
    public static final String MOD_ID = "meteor-autosell";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static AutoSellMeteor INSTANCE;

    // Core systems
    private ModConfig config;
    private AutoSell autoSell;
    private AutoReturn autoReturn;
    private AutoCoordinates autoCoordinates;
    private AdminDetector adminDetector;
    private PlayerDetector playerDetector;
    private WhitelistManager whitelistManager;
    private SellTracker sellTracker;
    private DiscordWebhook discordWebhook;

    // Keybindings
    private KeyBinding openConfigKey;
    private KeyBinding toggleAutoSellKey;
    private KeyBinding toggleAutoReturnKey;
    private KeyBinding toggleStatusHudKey;

    /** Whether the mod is fully initialized and ready to tick. */
    private boolean enabled = false;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        LOGGER.info("Meteor AutoSell v1.0.0 initializing...");

        try {
            // Load configuration from disk
            config = ModConfig.load();

            // Initialize all feature systems
            whitelistManager = new WhitelistManager(config);
            sellTracker = new SellTracker(config);
            discordWebhook = new DiscordWebhook(config);
            autoSell = new AutoSell(config, sellTracker, discordWebhook);
            autoReturn = new AutoReturn(config);
            adminDetector = new AdminDetector(config, discordWebhook);
            playerDetector = new PlayerDetector(config, whitelistManager, discordWebhook);

            registerKeyBindings();
            registerEvents();

            enabled = true;
            LOGGER.info("Meteor AutoSell loaded successfully!");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Meteor AutoSell", e);
        }
    }

    /** Register all keybindings for the mod. */
    private void registerKeyBindings() {
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.meteor-autosell.openConfig",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.meteor-autosell"
        ));

        toggleAutoSellKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.meteor-autosell.toggleAutoSell",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "category.meteor-autosell"
        ));

        toggleAutoReturnKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.meteor-autosell.toggleAutoReturn",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.meteor-autosell"
        ));

        toggleStatusHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.meteor-autosell.toggleStatusHud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.meteor-autosell"
        ));
    }

    /** Register client tick and HUD render events. */
    private void registerEvents() {
        // Main tick event - handles keybindings and feature updates
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                if (client.player == null || client.world == null) return;
                handleKeyBindings(client);
                tickFeatures(client);
            } catch (Exception e) {
                LOGGER.error("Error in tick event", e);
            }
        });

        // HUD render event - draws the status overlay
        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            try {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null || config == null || !config.showStatusHud) return;
                renderOverlay(drawContext);
            } catch (Exception e) {
                LOGGER.error("Error rendering HUD", e);
            }
        });
    }

    /** Process keybinding inputs each tick. */
    private void handleKeyBindings(MinecraftClient client) {
        try {
            // Open config screen
            while (openConfigKey.wasPressed()) {
                client.setScreen(new ConfigScreen(null, config));
            }

            // Toggle auto sell
            while (toggleAutoSellKey.wasPressed()) {
                config.autoSellEnabled = !config.autoSellEnabled;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal(
                            config.autoSellEnabled ? "[Meteor AutoSell] Enabled" : "[Meteor AutoSell] Disabled"
                    ), false);
                }
                if (config.autoSellEnabled) {
                    autoSell.start(client.player);
                } else {
                    autoSell.stop();
                }
                config.save();
            }

            // Toggle auto return
            while (toggleAutoReturnKey.wasPressed()) {
                config.autoReturnEnabled = !config.autoReturnEnabled;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal(
                            config.autoReturnEnabled ? "[Auto Return] Enabled" : "[Auto Return] Disabled"
                    ), false);
                }
                if (config.autoReturnEnabled) {
                    autoReturn.recordPosition(client.player);
                } else {
                    autoReturn.stop();
                }
                config.save();
            }

            // Toggle status HUD (resets session stats when turned on)
            while (toggleStatusHudKey.wasPressed()) {
                config.showStatusHud = !config.showStatusHud;
                if (config.showStatusHud) {
                    sellTracker.resetSession();
                }
                if (client.player != null) {
                    client.player.sendMessage(Text.literal(
                            config.showStatusHud ? "[Meteor AutoSell] Status HUD: ON" : "[Meteor AutoSell] Status HUD: OFF"
                    ), false);
                }
                config.save();
            }
        } catch (Exception e) {
            LOGGER.error("Error handling keybindings", e);
        }
    }

    /** Update all active features each tick. */
    private void tickFeatures(MinecraftClient client) {
        try {
            if (!enabled) return;

            if (config.autoSellEnabled) {
                autoSell.tick(client.player);
                AutoCoordinates.onTick(config, autoSell);
            }
            if (config.autoReturnEnabled && !autoSell.isRunning()) {
                autoReturn.tick(client.player);
            }
            if (config.adminDetectionEnabled) {
                adminDetector.tick(client.world);
            }
            if (config.playerDetectionEnabled) {
                playerDetector.tick(client.world, client.player, config.autoSellEnabled);
            }
            if (config.discordTrackingEnabled) {
                discordWebhook.tick(client.player, sellTracker);
            }
        } catch (Exception e) {
            LOGGER.error("Error ticking features", e);
        }
    }

    // ==================== HUD Rendering ====================

    /** Render the status HUD overlay on screen. */
    private void renderOverlay(net.minecraft.client.gui.DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || config == null || !config.showStatusHud) return;

        // HUD dimensions
        int x = 8, y = 8;
        int hudWidth = 210;
        int cardHeight = 38, cardGap = 4;
        int totalHeight = 14 + 6 + cardHeight + cardGap + cardHeight + 8 + 10 + 4;

        // Dark background with subtle scanlines
        context.fill(x, y, x + hudWidth, y + totalHeight, 0xF00A0D12);
        for (int sy = y; sy < y + totalHeight; sy += 6) {
            context.fill(x, sy, x + hudWidth, sy + 1, 0x0600FFB2);
        }

        int curY = y + 3;

        // Title bar
        context.fill(x + 2, curY, x + hudWidth - 2, curY + 1, 0x5500FFB2);
        curY += 4;
        context.drawTextWithShadow(client.textRenderer, Text.literal("METEOR AUTOSELL  --  HUD v2.0"), x + 8, curY, 0xFF00FFB2);
        curY += 12;
        context.fill(x + 2, curY, x + hudWidth - 2, curY + 1, 0x5500FFB2);
        curY += 6;

        // Top row: Money | Total Sell | $/hour
        int cardWidth = (hudWidth - 8 - cardGap * 2) / 3;
        int card1X = x + 4;
        int card2X = card1X + cardWidth + cardGap;
        int card3X = card2X + cardWidth + cardGap;

        drawCard(context, client, card1X, curY, cardWidth, cardHeight,
                "MONEY", sellTracker.formatCurrency(sellTracker.getSessionMoney()), "SESSION",
                0xFF00FFB2, 0x2200FFB2);
        drawCard(context, client, card2X, curY, cardWidth, cardHeight,
                "TOTAL SELL", String.valueOf(sellTracker.getSessionSold()), "SESSION",
                0xFF00D4FF, 0x2200D4FF);
        drawCard(context, client, card3X, curY, cardWidth, cardHeight,
                "$/HOUR", sellTracker.formatCurrency(sellTracker.getMoneyPerHour()), "AVERAGE",
                0xFFFBBF24, 0x22FBBF24);

        curY += cardHeight + cardGap;

        // Bottom row: Uptime | Status
        int botCardWidth = (hudWidth - 8 - cardGap) / 2;
        int bot1X = x + 4;
        int bot2X = bot1X + botCardWidth + cardGap;

        drawCard(context, client, bot1X, curY, botCardWidth, cardHeight,
                "UPTIME", sellTracker.getSessionTimeFormatted(), "HH:MM:SS",
                0xFFC084FC, 0x22C084FC);
        drawStatusCard(context, client, bot2X, curY, botCardWidth, cardHeight);

        curY += cardHeight + 6;

        // Footer
        context.fill(x + 8, curY, x + hudWidth - 8, curY + 1, 0x3300FFB2);
        curY += 4;
        String sessionNum = String.format("#%04d", (int)(sellTracker.getSessionElapsedMs() / 60000) + 1);
        context.drawTextWithShadow(client.textRenderer, Text.literal("SESSION " + sessionNum), x + 8, curY, 0x551E3A4A);
        String clock = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(clock), x + hudWidth / 2, curY, 0x661E4A5A);
        context.drawTextWithShadow(client.textRenderer, Text.literal("METEOR SYSTEM"), x + hudWidth - 90, curY, 0x5500FFB2);
    }

    /** Draw a stat card with label, value, and subtitle. */
    private void drawCard(net.minecraft.client.gui.DrawContext ctx, MinecraftClient client,
                          int x, int y, int w, int h,
                          String label, String value, String sub,
                          int accentColor, int bgColor) {
        ctx.fill(x, y, x + w, y + h, 0xF00E1520);
        ctx.fill(x, y, x + w, y + 1, bgColor);
        ctx.fill(x, y, x + 1, y + h, bgColor);
        ctx.fill(x + w - 1, y, x + w, y + h, bgColor);
        ctx.fill(x, y + h - 1, x + w, y + h, bgColor);
        ctx.fill(x + 3, y + 2, x + w - 3, y + 3, accentColor);
        ctx.drawTextWithShadow(client.textRenderer, Text.literal(label), x + 6, y + 6, 0xFF4A6A7A);
        ctx.drawTextWithShadow(client.textRenderer, Text.literal(value), x + 6, y + 17, accentColor);
        ctx.drawTextWithShadow(client.textRenderer, Text.literal(sub), x + 6, y + 28, 0xFF3A5A6A);
    }

    /** Draw the status card with feature toggle indicators. */
    private void drawStatusCard(net.minecraft.client.gui.DrawContext ctx, MinecraftClient client,
                                int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, 0xF00E1520);
        ctx.fill(x, y, x + w, y + 1, 0x2260A5FA);
        ctx.fill(x, y, x + 1, y + h, 0x2260A5FA);
        ctx.fill(x + w - 1, y, x + w, y + h, 0x2260A5FA);
        ctx.fill(x, y + h - 1, x + w, y + h, 0x2260A5FA);
        ctx.fill(x + 3, y + 2, x + w - 3, y + 3, 0xFF60A5FA);

        ctx.drawTextWithShadow(client.textRenderer, Text.literal("STATUS"), x + 6, y + 6, 0xFF4A6A7A);

        boolean sellOn = config.autoSellEnabled;
        ctx.fill(x + 6, y + 17, x + 12, y + 23, sellOn ? 0xFF00FFB2 : 0xFF374151);
        ctx.drawTextWithShadow(client.textRenderer, Text.literal("SELL"), x + 15, y + 17, sellOn ? 0xFF00FFB2 : 0xFF555555);

        boolean adminOn = config.adminDetectionEnabled;
        ctx.fill(x + 55, y + 17, x + 61, y + 23, adminOn ? 0xFFFBBF24 : 0xFF374151);
        ctx.drawTextWithShadow(client.textRenderer, Text.literal("ADMIN"), x + 64, y + 17, adminOn ? 0xFFFBBF24 : 0xFF555555);

        boolean returnOn = config.autoReturnEnabled;
        ctx.fill(x + 6, y + 28, x + 12, y + 34, returnOn ? 0xFF00FFB2 : 0xFF374151);
        ctx.drawTextWithShadow(client.textRenderer, Text.literal("RETURN"), x + 15, y + 28, returnOn ? 0xFF00FFB2 : 0xFF555555);
    }

    // ==================== Accessors ====================

    public static AutoSellMeteor getInstance() { return INSTANCE; }
    public ModConfig getConfig() { return config; }
    public AutoSell getAutoSell() { return autoSell; }
    public AutoReturn getAutoReturn() { return autoReturn; }
    public SellTracker getSellTracker() { return sellTracker; }
    public AdminDetector getAdminDetector() { return adminDetector; }
    public PlayerDetector getPlayerDetector() { return playerDetector; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
