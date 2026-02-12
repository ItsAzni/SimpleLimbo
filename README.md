# SimpleLimbo

A lightweight virtual limbo server plugin for Velocity proxy using [LimboAPI](https://github.com/Elytrium/LimboAPI).

## Features

- **Multiple Limbo Servers** - Create unlimited virtual limbo instances with different configurations
- **Auth Plugin Support** - Compatible with JPremium and other authentication plugins
- **Fallback System** - Automatically catch players when backend servers go down
- **AFK Detection** - Move idle players to a dedicated limbo server
- **Custom Worlds** - Load schematic files for limbo environments
- **Rich Display** - Titles, action bars, boss bars, and chat messages
- **Auto Reconnect** - Automatically attempt to reconnect players to backend servers
- **Velocity Bridge** - Register virtual servers that redirect to limbos

## Requirements

- Velocity 3.3.0+
- [LimboAPI](https://github.com/Elytrium/LimboAPI) 1.1.27+
- Java 17+

## Installation

1. Download LimboAPI and place it in your Velocity plugins folder
2. Download SimpleLimbo and place it in your Velocity plugins folder
3. Restart Velocity
4. Configure `plugins/simplelimbo/config.yml`

## Configuration

### Basic Limbo Setup

```yaml
limbos:
  auth:
    enabled: true
    dimension: "OVERWORLD"
    gamemode: "ADVENTURE"
    world-time: 6000

    spawn:
      x: 0.0
      y: 100.0
      z: 0.0
      yaw: 0.0
      pitch: 0.0

    settings:
      read-timeout: 30000
      should-rejoin: true
      should-respawn: false
      disable-falling: true
      disable-falling-delay-ms: 5000

    commands:
      - "login"
      - "register"

    display:
      on-join:
        chat: "&eUse /login or /register"
        title:
          enabled: true
          title: "&eAuthentication"
          subtitle: "&7Please authenticate"
```

### Velocity Bridge (Auth Plugin Compatibility)

For plugins like JPremium that expect players to be on a named server:

```yaml
velocity-bridge:
  enabled: true
  register-aliases: true
  aliases:
    auth: "auth" # Maps server name "auth" to limbo "auth"
```

Then in your auth plugin config, set the limbo server to `auth`.

### Anti-Fall Tuning

- `disable-falling` keeps players from dropping after join (Y lock mode).
- `disable-falling-delay-ms` controls when anti-fall starts.
- If players get slow join or "Loading Terrain" delay, increase `disable-falling-delay-ms` (for example: `1200`, `1500`, `2000`).

### Settings Notes

- `should-rejoin: true` keeps LimboAPI rejoin flow enabled (recommended for stability).
- `should-respawn: false` avoids extra respawn packet during join.

### Fallback System

Automatically catch players when backend servers crash:

```yaml
auto-triggers:
  fallback:
    enabled: true
    limbo: "fallback"
    kick-patterns:
      - ".*server.*closed.*"
      - ".*timed out.*"
    message: "&cServer offline. Moved to fallback."
```

### AFK System

Move idle players to a dedicated limbo:

```yaml
auto-triggers:
  afk:
    enabled: true
    limbo: "afk"
    idle-time: 300
    check-interval: 30
    exempt-permission: "simplelimbo.afk.exempt"
```

### Custom World Files

Load schematic files for limbo environments:

```yaml
limbos:
  lobby:
    world-file:
      enabled: true
      type: "SCHEMATIC"
      path: "plugins/simplelimbo/worlds/lobby.schem"
      offset:
        x: 0
        y: 64
        z: 0
      light-level: 15
```

## Commands

| Command                              | Permission          | Description              |
| ------------------------------------ | ------------------- | ------------------------ |
| `/simplelimbo send <player> <limbo>` | `simplelimbo.admin` | Send a player to a limbo |
| `/simplelimbo list`                  | `simplelimbo.admin` | List all limbo servers   |
| `/simplelimbo reload`                | `simplelimbo.admin` | Reload configuration     |

## Permissions

| Permission               | Description              |
| ------------------------ | ------------------------ |
| `simplelimbo.admin`      | Access to admin commands |
| `simplelimbo.afk.exempt` | Exempt from AFK trigger  |

## Auth Plugin Compatibility

SimpleLimbo includes compatibility features for authentication plugins that call `player.getCurrentServer()`:

1. Set `fake-server-name` in your limbo config to a valid Velocity server name
2. Enable the velocity-bridge with matching alias
3. Configure your auth plugin to use this server name as the limbo server

Example for JPremium:

```yaml
# SimpleLimbo config.yml
velocity-bridge:
  aliases:
    auth: "auth"

limbos:
  auth:
    fake-server-name: "auth"
    # ... other settings
```

```yaml
# JPremium configuration.yml
limboServerNames: [auth]
mainServerNames: [lobby]
```

## Building from Source

```bash
git clone https://github.com/yourusername/SimpleLimbo.git
cd SimpleLimbo
./gradlew shadowJar
```

The compiled JAR will be in `build/libs/`.

## License

MIT License

## Credits

- [LimboAPI](https://github.com/Elytrium/LimboAPI) by Elytrium
- [Velocity](https://github.com/PaperMC/Velocity) by PaperMC
