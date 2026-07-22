package com.autosellmeteor.features;

/**
 * Core auto sell feature using a state machine.
 *
 * Flow: SENDING_COMMAND -> WAITING_GUI -> MOVING_ITEMS -> CLICKING_SELL -> WAITING_AFTER_SELL -> repeat
 *
 * The mod sends /sell command, waits for the GUI to open, shift-clicks items
 * from player inventory into the sell slots, then clicks the confirm button.
 *
 * @author Meteor Team
 * @license MIT
 */
import com.autosellmeteor.AutoSellMeteor;
import com.autosellmeteor.config.ModConfig;
import com.autosellmeteor.discord.DiscordWebhook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.Random;

public class AutoSell {

    /** States of the auto sell state machine. */
    private enum SellState {
        IDLE,              // Not selling
        SENDING_COMMAND,   // Sending /sell command
        WAITING_GUI,       // Waiting for sell GUI to open
        MOVING_ITEMS,      // Moving items from inventory to sell slots
        CLICKING_SELL,     // Clicking the confirm sell button
        WAITING_AFTER_SELL // Waiting after sell before next cycle
    }

    private final ModConfig config;
    private final SellTracker sellTracker;
    private final DiscordWebhook discordWebhook;
    private final Random random = new Random();

    private SellState state = SellState.IDLE;
    private long delayUntilMs = 0;
    private long nextAllowedSellTimeMs = 0;
    private int lastSyncId = -1;
    private int guiRetries = 0;
    private long guiWaitDeadlineMs = 0;
    private int itemsSoldThisRound = 0;
    private int itemsInGui = 0;

    private static final long GUI_WAIT_TIMEOUT_MS = 5000;
    private static final int MAX_GUI_RETRIES = 3;

    public AutoSell(ModConfig config, SellTracker sellTracker, DiscordWebhook discordWebhook) {
        this.config = config;
        this.sellTracker = sellTracker;
        this.discordWebhook = discordWebhook;
    }

    // ==================== Lifecycle ====================

    /** Start auto selling. Saves position for coordinate tracking. */
    public void start(ClientPlayerEntity player) {
        state = SellState.SENDING_COMMAND;
        delayUntilMs = 0;
        guiRetries = 0;
        lastSyncId = -1;
        nextAllowedSellTimeMs = 0;
        itemsSoldThisRound = 0;
        itemsInGui = 0;

        // Reset session stats
        sellTracker.resetSession();

        // Save current position for auto coordinates
        AutoCoordinates.savePosition();

        // If GUI is already open, skip to moving items
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof GenericContainerScreen) {
            state = SellState.MOVING_ITEMS;
        }

        if (player != null) {
            player.sendMessage(Text.literal("[Meteor AutoSell] Auto sell started!"), false);
        }
    }

    /** Stop auto selling and close any open sell GUI. */
    public void stop() {
        state = SellState.IDLE;
        AutoCoordinates.clearPosition();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.currentScreen instanceof GenericContainerScreen) {
            client.player.closeScreen();
        }
    }

    /** Pause auto sell (used by coordinate detection when out of range). */
    public void pause() {
        if (state != SellState.IDLE) {
            state = SellState.IDLE;
        }
    }

    /** Resume auto sell after being paused. */
    public void resume() {
        if (state == SellState.IDLE && config.autoSellEnabled) {
            state = SellState.SENDING_COMMAND;
        }
    }

    /** Check if auto sell is currently running. */
    public boolean isRunning() {
        return state != SellState.IDLE;
    }

    // ==================== Tick ====================

    /** Main tick method - advances the state machine each tick. */
    public void tick(ClientPlayerEntity player) {
        try {
            if (state == SellState.IDLE || !config.autoSellEnabled) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            if (client.getNetworkHandler() == null) return;

            // Respect delay timers
            if (System.currentTimeMillis() < delayUntilMs) return;

            switch (state) {
                case SENDING_COMMAND -> handleSendCommand(client);
                case WAITING_GUI -> handleWaitGui(client);
                case MOVING_ITEMS -> handleMoveItems(client);
                case CLICKING_SELL -> handleClickSell(client);
                case WAITING_AFTER_SELL -> handleAfterSell(client);
            }
        } catch (Exception e) {
            AutoSellMeteor.LOGGER.error("Error in auto sell tick", e);
        }
    }

    // ==================== State Handlers ====================

    /** Send the /sell command to open the sell GUI. */
    private void handleSendCommand(MinecraftClient client) {
        GenericContainerScreen gui = getCurrentGui(client);
        lastSyncId = (gui != null) ? gui.getScreenHandler().syncId : -1;

        client.player.networkHandler.sendChatCommand(config.sellCommand);
        startGuiWait();
        state = SellState.WAITING_GUI;
    }

    /** Wait for the sell GUI to open with timeout and retry logic. */
    private void handleWaitGui(MinecraftClient client) {
        GenericContainerScreen gui = getCurrentGui(client);
        if (gui != null && gui.getScreenHandler().syncId != lastSyncId) {
            // GUI opened successfully
            guiRetries = 0;
            setDelay(config.sellCommandDelayTicks * 50L);
            state = SellState.MOVING_ITEMS;
            return;
        }

        if (isGuiTimedOut()) {
            guiRetries++;
            if (guiRetries >= MAX_GUI_RETRIES) {
                guiRetries = 0;
                nextAllowedSellTimeMs = System.currentTimeMillis() + 10000;
                state = SellState.IDLE;
                MinecraftClient.getInstance().player.sendMessage(
                        Text.literal("[Meteor AutoSell] Failed to open sell GUI after 3 retries"), false);
                return;
            }
            state = SellState.SENDING_COMMAND;
        }
    }

    /** Move items from player inventory to sell GUI slots using shift-click. */
    private void handleMoveItems(MinecraftClient client) {
        if (System.currentTimeMillis() < nextAllowedSellTimeMs) return;

        GenericContainerScreen gui = getCurrentGui(client);
        if (gui == null) {
            state = SellState.SENDING_COMMAND;
            return;
        }

        var screenHandler = gui.getScreenHandler();
        int containerSize = screenHandler.slots.size();
        if (containerSize < 36) {
            state = SellState.SENDING_COMMAND;
            return;
        }

        // If no free slots in container, go to sell
        if (countFreeContainerSlots(screenHandler) == 0) {
            state = SellState.CLICKING_SELL;
            return;
        }

        // Move items from player inventory to container
        int playerStart = containerSize - 36;
        int playerEnd = containerSize - 1;
        int moved = 0;

        for (int i = playerStart; i <= playerEnd && moved < config.itemsPerTick; i++) {
            ItemStack stack = screenHandler.getSlot(i).getStack();
            if (!stack.isEmpty()) {
                int count = stack.getCount();
                client.interactionManager.clickSlot(
                        screenHandler.syncId, i, 0, SlotActionType.QUICK_MOVE, client.player
                );
                moved++;
                itemsInGui += count;
            }
        }

        if (moved > 0) {
            setDelay(config.moveDelayTicks * 50L);
        } else {
            if (countFreeContainerSlots(screenHandler) == 0) {
                state = SellState.CLICKING_SELL;
            } else {
                syncInventory(client);
            }
        }
    }

    /** Click the confirm sell button (slot 53) and record sold items. */
    private void handleClickSell(MinecraftClient client) {
        GenericContainerScreen gui = getCurrentGui(client);
        if (gui == null) {
            state = SellState.SENDING_COMMAND;
            return;
        }

        var screenHandler = gui.getScreenHandler();

        // If there are still free slots, go back to moving items
        if (countFreeContainerSlots(screenHandler) > 0) {
            state = SellState.MOVING_ITEMS;
            return;
        }

        // Click confirm button at slot 53
        if (screenHandler.slots.size() > 53) {
            client.interactionManager.clickSlot(
                    screenHandler.syncId, 53, 0, SlotActionType.PICKUP, client.player
            );

            // Record the sold items
            sellTracker.addSold(itemsInGui, "items");
            itemsSoldThisRound += itemsInGui;
        }

        itemsInGui = 0;
        setDelay(config.sellDelayTicks * 50L);
        state = SellState.WAITING_AFTER_SELL;
    }

    /** Handle post-sell: sync inventory and go back to moving items. */
    private void handleAfterSell(MinecraftClient client) {
        syncInventory(client);
        state = SellState.MOVING_ITEMS;
    }

    // ==================== Helpers ====================

    private GenericContainerScreen getCurrentGui(MinecraftClient client) {
        if (client.currentScreen instanceof GenericContainerScreen gui) return gui;
        return null;
    }

    private int countFreeContainerSlots(net.minecraft.screen.ScreenHandler screenHandler) {
        int maxCheck = Math.min(45, screenHandler.slots.size());
        int free = 0;
        for (int i = 0; i < maxCheck; i++) {
            if (screenHandler.getSlot(i).getStack().isEmpty()) free++;
        }
        return free;
    }

    private void syncInventory(MinecraftClient client) {
        if (client.player == null) return;
        GenericContainerScreen gui = getCurrentGui(client);
        if (gui == null) return;
        client.interactionManager.clickSlot(
                gui.getScreenHandler().syncId, -999, 0, SlotActionType.PICKUP, client.player
        );
    }

    private void setDelay(long ms) {
        delayUntilMs = System.currentTimeMillis() + ms;
    }

    private void startGuiWait() {
        guiWaitDeadlineMs = System.currentTimeMillis() + GUI_WAIT_TIMEOUT_MS;
    }

    private boolean isGuiTimedOut() {
        return System.currentTimeMillis() >= guiWaitDeadlineMs;
    }

    public int getTotalSoldThisRound() {
        return itemsSoldThisRound;
    }
}
