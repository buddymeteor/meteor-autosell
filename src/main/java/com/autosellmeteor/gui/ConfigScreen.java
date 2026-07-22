package com.autosellmeteor.gui;

/**
 * Configuration GUI screen for Meteor AutoSell.
 *
 * Provides a multi-page interface for configuring all mod settings:
 * - Home page: Auto sell, HUD, sell settings
 * - Admin page: Admin detection, player detection, whitelist
 * - Discord page: Webhook settings, tracking
 *
 * @author Meteor Team
 * @license MIT
 */
import com.autosellmeteor.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private final ModConfig config;

    // Text fields
    private TextFieldWidget webhookField;
    private TextFieldWidget sellCommandField;
    private TextFieldWidget itemsPerTickField;
    private TextFieldWidget moveDelayField;
    private TextFieldWidget detectionRadiusField;
    private TextFieldWidget adminNamesField;
    private TextFieldWidget whitelistField;

    // Page navigation
    private int page = 0;
    private static final int PAGE_HOME = 0;
    private static final int PAGE_ADMIN = 1;
    private static final int PAGE_DISCORD = 2;

    public ConfigScreen(Screen parent, ModConfig config) {
        super(Text.literal("Meteor AutoSell Config"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        switch (page) {
            case PAGE_HOME -> initHomePage();
            case PAGE_ADMIN -> initAdminPage();
            case PAGE_DISCORD -> initDiscordPage();
        }
    }

    // ==================== Home Page ====================

    private void initHomePage() {
        int cx = this.width / 2;
        int sy = 40;
        int sp = 28;

        addToggle(cx, sy, sp, 0, "Auto Sell", config.autoSellEnabled, v -> config.autoSellEnabled = v);
        addToggle(cx, sy, sp, 1, "Status HUD", config.showStatusHud, v -> config.showStatusHud = v);
        addToggle(cx, sy, sp, 2, "Skip Tools", config.skipTools, v -> config.skipTools = v);
        addToggle(cx, sy, sp, 3, "Skip Armor", config.skipArmor, v -> config.skipArmor = v);
        addToggle(cx, sy, sp, 4, "Auto Return", config.autoReturnEnabled, v -> config.autoReturnEnabled = v);

        sellCommandField = addTextField(cx, sy, sp, 5, "Sell Command", config.sellCommand, v -> config.sellCommand = v);
        itemsPerTickField = addTextFieldHalf(cx, sy, sp, 6, true, "Items/Tick", String.valueOf(config.itemsPerTick), v -> { try { config.itemsPerTick = Integer.parseInt(v); } catch (NumberFormatException ignored) {} });
        moveDelayField = addTextFieldHalf(cx, sy, sp, 6, false, "Move Delay", String.valueOf(config.moveDelayTicks), v -> { try { config.moveDelayTicks = Integer.parseInt(v); } catch (NumberFormatException ignored) {} });

        addButton(cx, sy, sp, 7, "Admin Settings >", () -> { page = PAGE_ADMIN; clearAndInit(); });
        addButton(cx, sy, sp, 8, "Discord Settings >", () -> { page = PAGE_DISCORD; clearAndInit(); });
        addButton(cx, sy, sp, 9, "Close", () -> this.close());
    }

    // ==================== Admin Page ====================

    private void initAdminPage() {
        int cx = this.width / 2;
        int sy = 40;
        int sp = 28;

        addToggle(cx, sy, sp, 0, "Admin Detection", config.adminDetectionEnabled, v -> config.adminDetectionEnabled = v);
        addToggle(cx, sy, sp, 1, "Disconnect on Admin", config.disconnectOnAdmin, v -> config.disconnectOnAdmin = v);

        adminNamesField = addTextField(cx, sy, sp, 2, "Admin Names", String.join(", ", config.adminNames), v -> {
            config.adminNames = v.split(",");
            for (int i = 0; i < config.adminNames.length; i++) config.adminNames[i] = config.adminNames[i].trim();
        });

        addToggle(cx, sy, sp, 3, "Player Detection", config.playerDetectionEnabled, v -> config.playerDetectionEnabled = v);
        addToggle(cx, sy, sp, 4, "Disconnect on Player", config.disconnectOnPlayer, v -> config.disconnectOnPlayer = v);

        detectionRadiusField = addTextField(cx, sy, sp, 5, "Detection Radius", String.valueOf(config.detectionRadius), v -> { try { config.detectionRadius = Double.parseDouble(v); } catch (NumberFormatException ignored) {} });

        whitelistField = addTextField(cx, sy, sp, 6, "Whitelist", String.join(", ", config.whitelistPlayers), v -> {
            config.whitelistPlayers = v.split(",");
            for (int i = 0; i < config.whitelistPlayers.length; i++) config.whitelistPlayers[i] = config.whitelistPlayers[i].trim();
        });
        whitelistField.setPlaceholder(Text.literal("Player1, Player2, ...").styled(s -> s.withColor(0xFF555555)));

        addButton(cx, sy, sp, 7, "< Back", () -> { page = PAGE_HOME; clearAndInit(); });
        addButton(cx, sy, sp, 8, "Close", () -> this.close());
    }

    // ==================== Discord Page ====================

    private void initDiscordPage() {
        int cx = this.width / 2;
        int sy = 40;
        int sp = 28;

        addToggle(cx, sy, sp, 0, "Discord Tracking", config.discordTrackingEnabled, v -> config.discordTrackingEnabled = v);

        webhookField = addTextField(cx, sy, sp, 1, "Discord Webhook URL", config.discordWebhookUrl, v -> config.discordWebhookUrl = v);

        addButton(cx, sy, sp, 2, "Reset Stats", () -> { config.totalSold = 0; config.totalMoney = 0; config.save(); });
        addButton(cx, sy, sp, 3, "< Back", () -> { page = PAGE_HOME; clearAndInit(); });
        addButton(cx, sy, sp, 4, "Close", () -> this.close());
    }

    // ==================== Helper Methods ====================

    private void addToggle(int cx, int sy, int sp, int row, String label, boolean value, java.util.function.Consumer<Boolean> setter) {
        addDrawableChild(ButtonWidget.builder(
                Text.literal(label + ": " + (value ? "ON" : "OFF")),
                button -> {
                    setter.accept(!value);
                    button.setMessage(Text.literal(label + ": " + (!value ? "ON" : "OFF")));
                    config.save();
                }
        ).dimensions(cx - 100, sy + sp * row, 200, 20).build());
    }

    private void addButton(int cx, int sy, int sp, int row, String label, Runnable action) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> {
            action.run();
            config.save();
        }).dimensions(cx - 100, sy + sp * row, 200, 20).build());
    }

    private TextFieldWidget addTextField(int cx, int sy, int sp, int row, String label, String value, java.util.function.Consumer<String> setter) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, cx - 100, sy + sp * row + 5, 200, 20, Text.literal(label));
        field.setText(value);
        field.setChangedListener(setter::accept);
        addDrawableChild(field);
        return field;
    }

    private TextFieldWidget addTextFieldHalf(int cx, int sy, int sp, int row, boolean left, String label, String value, java.util.function.Consumer<String> setter) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, left ? cx - 100 : cx + 10, sy + sp * row + 5, 90, 20, Text.literal(label));
        field.setText(value);
        field.setChangedListener(setter::accept);
        addDrawableChild(field);
        return field;
    }

    // ==================== Render ====================

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int cx = this.width / 2;

        context.drawCenteredTextWithShadow(this.textRenderer, "Meteor AutoSell Config", cx, 15, 0xFFFF55);
        context.drawCenteredTextWithShadow(this.textRenderer, "RightShift=Config | J=AutoSell | K=Return | H=HUD", cx, 28, 0xAAAAAA);

        int sy = 40;
        int sp = 28;

        if (page == PAGE_HOME) {
            context.drawTextWithShadow(this.textRenderer, "Sell Command:", cx - 100, sy + sp * 5 - 5, 0xAAAAAA);
            context.drawTextWithShadow(this.textRenderer, "Items/Tick:", cx - 100, sy + sp * 6 - 5, 0xAAAAAA);
            context.drawTextWithShadow(this.textRenderer, "Move Delay:", cx + 10, sy + sp * 6 - 5, 0xAAAAAA);
        } else if (page == PAGE_ADMIN) {
            context.drawTextWithShadow(this.textRenderer, "Admin Names (comma separated):", cx - 100, sy + sp * 2 - 5, 0xAAAAAA);
            context.drawTextWithShadow(this.textRenderer, "Scan radius (blocks, 8 chunks=128):", cx - 100, sy + sp * 5 - 5, 0xAAAAAA);
            context.drawTextWithShadow(this.textRenderer, "Whitelist (comma separated):", cx - 100, sy + sp * 6 - 5, 0xAAAAAA);
        } else if (page == PAGE_DISCORD) {
            context.drawTextWithShadow(this.textRenderer, "Discord Webhook URL:", cx - 100, sy + sp - 5, 0xAAAAAA);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() { this.client.setScreen(parent); }

    @Override
    public boolean shouldPause() { return true; }
}
