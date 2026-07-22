package com.autosellmeteor.features;

/**
 * Tracks money earned and items sold during the session.
 *
 * Parses money amounts from chat messages using regex.
 * Handles K (thousand), M (million), B (billion) suffixes.
 * Calculates money per hour from session duration.
 *
 * @author Meteor Team
 * @license MIT
 */
import com.autosellmeteor.config.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SellTracker {
    private final ModConfig config;

    private int sessionSold = 0;
    private double sessionMoney = 0.0;
    private long sessionStartTime = 0;
    private final List<SellRecord> history = new ArrayList<>();

    /** Regex pattern to match money amounts like $500, $1.5K, $2M */
    private static final Pattern MONEY_PATTERN = Pattern.compile(
            "(?i)(?:\\+\\s*)?\\$[ \\t]*([0-9]+(?:\\.[0-9]+)?)\\s*([KkMmBb]?)"
    );

    public SellTracker(ModConfig config) {
        this.config = config;
        this.sessionStartTime = System.currentTimeMillis();
    }

    /** Record items sold. */
    public void addSold(int count, String itemName) {
        if (sessionStartTime == 0) sessionStartTime = System.currentTimeMillis();
        sessionSold += count;
        config.totalSold += count;
        config.save();
        history.add(new SellRecord(count, itemName, 0, System.currentTimeMillis()));
    }

    /** Record money earned. */
    public void addMoney(double amount) {
        if (sessionStartTime == 0) sessionStartTime = System.currentTimeMillis();
        sessionMoney += amount;
        config.totalMoney += amount;
        config.save();
    }

    /**
     * Parse money amount from a chat message.
     * Returns true if money was found and added.
     */
    public boolean parseMoneyFromChat(String message) {
        if (message == null || message.isEmpty()) return false;

        try {
            Matcher matcher = MONEY_PATTERN.matcher(message);
            if (matcher.find()) {
                double amount = Double.parseDouble(matcher.group(1));
                String suffix = matcher.group(2).toUpperCase();

                switch (suffix) {
                    case "K" -> amount *= 1000.0;
                    case "M" -> amount *= 1000000.0;
                    case "B" -> amount *= 1000000000.0;
                }

                addMoney(amount);
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    // ==================== Getters ====================

    public int getTotalSold() { return config.totalSold; }
    public double getTotalMoney() { return config.totalMoney; }
    public int getSessionSold() { return sessionSold; }
    public double getSessionMoney() { return sessionMoney; }

    /** Calculate money earned per hour based on session duration. */
    public double getMoneyPerHour() {
        if (sessionStartTime == 0 || sessionMoney == 0) return 0;
        double elapsedHours = (System.currentTimeMillis() - sessionStartTime) / 3600000.0;
        if (elapsedHours < 0.001) return 0;
        return sessionMoney / elapsedHours;
    }

    public long getSessionElapsedMs() {
        return (sessionStartTime == 0) ? 0 : System.currentTimeMillis() - sessionStartTime;
    }

    /** Format time as HH:MM:SS. */
    public String getSessionTimeFormatted() {
        long elapsed = getSessionElapsedMs();
        return String.format("%02d:%02d:%02d",
                elapsed / 3600000,
                (elapsed % 3600000) / 60000,
                (elapsed % 60000) / 1000);
    }

    /** Format currency with K/M/B suffixes. */
    public String formatCurrency(double amount) {
        if (amount >= 1000000000.0) return String.format("$%.2fB", amount / 1000000000.0);
        if (amount >= 1000000.0) return String.format("$%.2fM", amount / 1000000.0);
        if (amount >= 1000.0) return String.format("$%.2fK", amount / 1000.0);
        return String.format("$%.0f", amount);
    }

    /** Reset session stats (called when toggling HUD or auto sell). */
    public void resetSession() {
        sessionSold = 0;
        sessionMoney = 0.0;
        sessionStartTime = System.currentTimeMillis();
        history.clear();
    }

    /** Record of a single sell transaction. */
    public static class SellRecord {
        public final int count;
        public final String itemName;
        public final double price;
        public final long timestamp;

        public SellRecord(int count, String itemName, double price, long timestamp) {
            this.count = count;
            this.itemName = itemName;
            this.price = price;
            this.timestamp = timestamp;
        }
    }
}
