# Meteor AutoSell

**Open Source Minecraft Fabric Mod** - Auto sell items via `/sell` GUI with anti-detection, admin/player detection, and Discord integration.

> This project is fully open source. You can inspect every line of code to verify there is no RAT, malware, spyware, backdoors, hidden network activity, or crypto miners.

## Features

| Feature | Description |
|---------|-------------|
| **Auto Sell** | Opens `/sell` GUI, transfers items, clicks confirm button automatically |
| **Admin Detection** | Detects players with star prefix (★☆✦) or custom admin names, auto-disconnects |
| **Player Detection** | Scans 8 chunk radius, disconnects when unknown player nearby |
| **Auto Return** | Returns to sell position when pushed away (1 block threshold) |
| **Auto Coordinates** | Pauses auto sell when moving away from sell spot |
| **Status HUD** | Real-time display: Money, Total Sell, $/hour, Uptime, Status |
| **Discord Integration** | Webhook notifications for sells, admin alerts, player alerts |
| **Whitelist** | Whitelist players to prevent false player detection triggers |

## Keybinds

| Key | Action |
|-----|--------|
| `Right Shift` | Open Config Screen |
| `J` | Toggle Auto Sell |
| `K` | Toggle Auto Return |
| `H` | Toggle Status HUD |

## Commands

| Command | Description |
|---------|-------------|
| `/mas config` | Open config screen |
| `/mas sell` | Toggle auto sell |
| `/mas return` | Toggle auto return |
| `/mas admin` | Toggle admin detection |
| `/mas hud` | Toggle status HUD |
| `/mas status` | Show current status |

## Configuration

All settings are saved to `config/meteor-autosell.json`:

```json
{
  "autoSellEnabled": false,
  "sellCommand": "sell",
  "itemsPerTick": 9,
  "moveDelayTicks": 1,
  "autoReturnEnabled": false,
  "adminDetectionEnabled": false,
  "disconnectOnAdmin": true,
  "adminNames": [],
  "playerDetectionEnabled": false,
  "disconnectOnPlayer": true,
  "detectionRadius": 128.0,
  "whitelistPlayers": [],
  "discordTrackingEnabled": false,
  "discordWebhookUrl": ""
}
```

## Build

### Requirements
- Java 21
- Gradle 8+

### Build Command
```bash
./gradlew build
```

The compiled JAR will be at `build/libs/meteor-autosell-1.0.0.jar`.

### Install
1. Install [Fabric Loader](https://fabricmc.net/) for Minecraft 1.21.1
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Copy `meteor-autosell-1.0.0.jar` to your `mods` folder

## Project Structure

```
src/main/java/com/autosellmeteor/
├── AutoSellMeteor.java              # Main mod entry point
├── config/
│   └── ModConfig.java              # Configuration management
├── features/
│   ├── AutoSell.java               # Core auto sell logic (state machine)
│   ├── AutoReturn.java             # Return to sell position
│   ├── AutoCoordinates.java        # Position tracking & range check
│   ├── AdminDetector.java          # Admin/star prefix detection
│   ├── PlayerDetector.java         # Nearby player detection
│   ├── WhitelistManager.java       # Whitelist management
│   └── SellTracker.java            # Money & items tracking
├── gui/
│   └── ConfigScreen.java           # Config GUI
├── discord/
│   └── DiscordWebhook.java         # Discord webhook integration
└── mixin/
    ├── MinecraftClientMixin.java   # Disconnect handler
    ├── PlayerEntityMixin.java      # Tick handler
    ├── ClientPlayNetworkHandlerMixin.java  # Chat money parsing
    └── InGameHudMixin.java         # HUD rendering
```

## How It Works

### Auto Sell Flow
1. **SENDING_COMMAND** - Sends `/sell` chat command
2. **WAITING_GUI** - Waits for sell GUI to open (5s timeout, 3 retries)
3. **MOVING_ITEMS** - Shift-clicks items from inventory to sell slots (0-44)
4. **CLICKING_SELL** - Clicks confirm button at slot 53
5. **WAITING_AFTER_SELL** - Waits before next cycle

### Admin Detection
- Checks player list for star prefix characters: ★ ☆ ✦ ✶ ⭐
- Also checks against custom admin names list
- Disconnects after 1.5s warning

### Player Detection
- Scans every 10 ticks (0.5s)
- Checks all players within 128 blocks (8 chunks)
- Skips whitelisted and dead players
- Stops auto sell and disconnects when detected

### Money Tracking
- Parses chat messages with regex: `\$([0-9]+(?:\.[0-9]+)?)\s*([KkMmBb]?)`
- Handles K (thousand), M (million), B (billion) suffixes
- Calculates money per hour from session data

## Security

### What's Open Source
- All client-side code
- All features and logic
- Configuration system
- HUD rendering
- Discord integration

### What's Private (Server-Side Only)
- License/authentication server (not included in this repo)
- The client only sends a license key to the auth server
- No API secrets, private keys, or auth secrets in client code

## License

MIT License - See [LICENSE](LICENSE) for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## Disclaimer

This mod is for educational purposes. Use at your own risk. The authors are not responsible for any consequences of using this mod on servers.
