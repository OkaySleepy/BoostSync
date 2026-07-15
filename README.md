<a id="readme-top"></a>

<div align="center">
  <h1>BoostSync</h1>

  <p><strong>Reward your Discord Nitro boosters in Minecraft — automatically, and only when they're actually boosting.</strong></p>
    </a>

  <p>
    Secure Discord account linking, automatic booster verification, temporary rank synchronization,
    seasonal rewards, and configurable renewals for Spigot and Paper servers.
      </a>
  </p>

  <!-- Modrinth -->
  <p>
    <a href="https://modrinth.com/plugin/boostsync#download">
        </a>
      <img src="https://img.shields.io/modrinth/dt/boostsync?style=for-the-badge&logo=modrinth&label=Downloads&color=1bd96a" alt="Modrinth Downloads">
    </a>
    <a href="https://modrinth.com/plugin/boostsync/versions">
        </a>
      <img src="https://img.shields.io/modrinth/v/boostsync?style=for-the-badge&logo=modrinth&label=Version&color=1565c0" alt="Latest Version">
    </a>
    <a href="https://modrinth.com/plugin/boostsync">
        </a>
      <img src="https://img.shields.io/badge/Modrinth-BoostSync-1bd96a?style=for-the-badge&logo=modrinth" alt="View on Modrinth">
    </a>
  </p>

  <!-- GitHub -->
  <p>
    <a href="https://github.com/OkaySleepy/BoostSync/releases">
        </a>
      <img src="https://img.shields.io/github/downloads/OkaySleepy/BoostSync/total?style=for-the-badge&logo=github&label=Downloads" alt="GitHub Downloads">
    </a>
    <a href="https://github.com/OkaySleepy/BoostSync/releases/latest">
        </a>
      <img src="https://img.shields.io/github/v/release/OkaySleepy/BoostSync?style=for-the-badge&logo=github&label=Version" alt="Latest Version">
    </a>
    <a href="https://github.com/OkaySleepy/BoostSync">
        </a>
      <img src="https://img.shields.io/badge/GitHub-BoostSync-181717?style=for-the-badge&logo=github" alt="View on GitHub">
    </a>
  </p>
</div>

---

---

# BoostSync

**Reward your Discord Nitro boosters in Minecraft — automatically, and only when they're actually boosting.**

BoostSync links a player's Minecraft account to their Discord account with a quick one-time code, checks whether they hold your server's Booster role, and hands out rewards plus a temporary in-game rank. From then on it keeps that rank in sync with Discord on its own: keep boosting and the rank keeps renewing, stop boosting and it quietly expires.

**Reward your Discord Nitro boosters in Minecraft --- automatically, and
only when they're actually boosting.**

BoostSync links a player's Minecraft account to their Discord account
with a quick one-time code, checks whether they hold your server's
Booster role, and hands out rewards plus a temporary in-game rank. From
then on it keeps that rank in sync with Discord on its own: keep
boosting and the rank keeps renewing, stop boosting and it quietly
expires.

------------------------------------------------------------------------

## Features

-   **Secure account linking** --- a short one-time code the player
    pastes into a Discord channel, tied to their real Discord account.
-   **Automatic role verification** --- checks your configured Booster
    role directly through the Discord bot.
-   **Temporary booster rank** --- granted on boost and renewed every
    cycle while the player keeps boosting.
-   **Auto-renewal & expiry** --- a background check renews active
    boosters and removes the rank from anyone who stopped.
-   **Once-per-season rewards** --- the one-time bonus can only be
    claimed once per season, even if a player unboosts and reboosts.
-   **Instant manual re-check** --- linked players just run `/booster`
    again to refresh on the spot instead of waiting.
-   **Fully configurable** --- every reward, rank command, cooldown,
    interval, and message lives in `config.yml`.

------------------------------------------------------------------------

```mermaid
flowchart LR

A["Player runs /booster"] --> B["Generate verification code"]
B --> C["Player sends code in Discord"]
C --> D{"Discord Bot"}

D -->|Booster| E["Link Account"]
E --> F["Give Rewards"]
F --> G["Grant Temporary Rank"]
G --> H["Automatic Sync Task"]

H --> I{"Still Boosting?"}
I -->|Yes| J["Renew Rank"]
I -->|No| K["Rank Expires"]

J --> H
```

------------------------------------------------------------------------

## Commands

  -----------------------------------------------------------------------
  Command                 Description             Permission
  ----------------------- ----------------------- -----------------------
  `/booster`              Verify your boost,      `boostsync.use`
                          claim rewards, and      
                          renew your rank         

  `/boostsync reload`     Reload the              `boostsync.admin`
                          configuration           

  `/boostsync help`       Show the help menu      `boostsync.admin`
  -----------------------------------------------------------------------

Aliases: `/boost`, `/boostreward`.

## Permissions

  Permission                    Description                      Default
  ----------------------------- -------------------------------- ----------
  `boostsync.use`               Use `/booster`                   everyone
  `boostsync.admin`             Full admin access                op
  `boostsync.reload`            Reload the config                op
  `boostsync.bypass`            Skip season limit and cooldown   false
  `boostsync.bypass.cooldown`   Skip only the command cooldown   false
  `boostsync.bypass.onetime`    Claim season rewards again       false

------------------------------------------------------------------------

## Setup

1.  Download BoostSync from Modrinth.
2.  Place the jar into `plugins/`.
3.  Configure your Discord bot.
4.  Configure rewards and rank commands.
5.  Run `/boostsync reload`.

------------------------------------------------------------------------

## Configuration highlights

``` yaml
season: 1

rank:
  duration-days: 7
  check-interval-minutes: 30

rewards:
  commands:
    - "give {player} diamond 64"
```

Placeholders: `{player}`, `{uuid}` in commands --- `{prefix}`,
`{player}`, `{code}`, `{days}` in messages.

------------------------------------------------------------------------

## Support

-   **Developer:** SleepyDN
-   **Email:** sleepyxemail@gmail.com
-   **GitHub:** https://github.com/OkaySleepy
-   **Discord:** @OkaySleepyX
