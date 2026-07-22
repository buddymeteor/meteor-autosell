package com.autosellmeteor.features;

/**
 * Manages the player whitelist for detection bypass.
 *
 * Whitelisted players will not trigger player detection alerts.
 * Can be edited via the config GUI or config file.
 *
 * @author Meteor Team
 * @license MIT
 */
import com.autosellmeteor.config.ModConfig;

import java.util.HashSet;
import java.util.Set;

public class WhitelistManager {
    private final ModConfig config;
    private final Set<String> whitelistedPlayers = new HashSet<>();

    public WhitelistManager(ModConfig config) {
        this.config = config;
        loadWhitelist();
    }

    /** Load whitelist from config. */
    private void loadWhitelist() {
        whitelistedPlayers.clear();
        if (config.whitelistPlayers != null) {
            for (String name : config.whitelistPlayers) {
                whitelistedPlayers.add(name.toLowerCase());
            }
        }
    }

    /** Check if a player is whitelisted. */
    public boolean isWhitelisted(String playerName) {
        return whitelistedPlayers.contains(playerName.toLowerCase());
    }

    /** Add a player to the whitelist. */
    public void addPlayer(String playerName) {
        whitelistedPlayers.add(playerName.toLowerCase());
        saveWhitelist();
    }

    /** Remove a player from the whitelist. */
    public void removePlayer(String playerName) {
        whitelistedPlayers.remove(playerName.toLowerCase());
        saveWhitelist();
    }

    /** Save whitelist back to config. */
    private void saveWhitelist() {
        config.whitelistPlayers = whitelistedPlayers.toArray(new String[0]);
        config.save();
    }

    public Set<String> getWhitelistedPlayers() { return new HashSet<>(whitelistedPlayers); }
    public void reload() { loadWhitelist(); }
}
