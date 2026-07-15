package com.boostsync.tasks;

import com.boostsync.BoostSync;
import com.boostsync.discord.DiscordBot;
import com.boostsync.manager.DataManager;
import com.boostsync.manager.RewardManager;
import java.util.UUID;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Runs on the configured interval (30 minutes by default) and keeps every
 * tracked booster rank in sync with Discord:
 *
 *  - still boosting -> push the rank expiry forward by the full duration (renew)
 *  - stopped boosting -> once the current window runs out, remove the rank
 *
 * A rank that has already expired is never renewed on its own; the player has to
 * run /booster again. The one-time season rewards are never re-granted here.
 */
public class RankTask extends BukkitRunnable {
   private final BoostSync plugin;

   public RankTask(BoostSync plugin) {
      this.plugin = plugin;
   }

   @Override
   public void run() {
      DiscordBot bot = this.plugin.getDiscordBot();
      if (bot == null) {
         return;
      }

      DataManager data = this.plugin.getDataManager();
      RewardManager rewards = this.plugin.getRewardManager();
      long now = System.currentTimeMillis();
      long durationMs = this.plugin.getConfig().getLong("rank.duration-days", 7L) * 86_400_000L;

      for (UUID uuid : data.getTrackedBoosters()) {
         long expiry = data.getRankExpiry(uuid);
         if (expiry <= 0L) {
            continue; // no active rank to manage
         }

         String discordId = data.getDiscordId(uuid);
         if (discordId == null) {
            continue;
         }

         try {
            if (bot.hasBoosterRole(discordId)) {
               data.setRankExpiry(uuid, now + durationMs); // still boosting: renew
            } else if (now >= expiry) {
               rewards.expireRank(uuid, data.getName(uuid)); // stopped boosting and window is up
            }
         } catch (Exception e) {
            this.plugin.getLogger().warning("Failed to refresh booster " + uuid + ": " + e.getMessage());
         }
      }
   }
}
