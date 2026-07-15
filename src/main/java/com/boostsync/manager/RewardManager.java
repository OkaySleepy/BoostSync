package com.boostsync.manager;

import com.boostsync.BoostSync;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;

/**
 * Central place for handing out (and taking away) booster perks.
 *
 * The temporary rank is granted once and tracked with an expiry timestamp.
 * While the player keeps boosting, the renewal task keeps pushing that expiry
 * forward. The one-time season rewards are only ever handed out once per season.
 */
public class RewardManager {
   private final BoostSync plugin;

   public RewardManager(BoostSync plugin) {
      this.plugin = plugin;
   }

   /**
    * Applies booster perks for a confirmed boost.
    *
    * @param forceRewards give the season rewards even if already claimed (bypass permission)
    * @return true if the one-time season rewards were newly granted
    */
   public boolean applyBoost(UUID uuid, String name, String discordId, boolean forceRewards) {
      DataManager data = this.plugin.getDataManager();
      int season = this.plugin.getConfig().getInt("season", 1);
      long durationMs = this.plugin.getConfig().getLong("rank.duration-days", 7L) * 86_400_000L;
      long now = System.currentTimeMillis();

      data.link(uuid, name, discordId);

      // Only run the grant commands when the rank isn't already active,
      // so we never stack duplicate permission nodes on renewal.
      boolean rankInactive = data.getRankExpiry(uuid) <= now;
      if (rankInactive) {
         this.runCommands("rank.grant-commands", name, uuid);
      }
      data.setRankExpiry(uuid, now + durationMs);

      boolean giveRewards = forceRewards || !data.hasClaimedThisSeason(uuid, season);
      if (giveRewards) {
         this.runCommands("rewards.commands", name, uuid);
         data.setSeasonClaimed(uuid, season);
      }
      return giveRewards;
   }

   /** Runs the expire commands and clears the tracked rank. */
   public void expireRank(UUID uuid, String name) {
      this.runCommands("rank.expire-commands", name, uuid);
      this.plugin.getDataManager().setRankExpiry(uuid, 0L);
   }

   private void runCommands(String path, String name, UUID uuid) {
      List<String> commands = this.plugin.getConfig().getStringList(path);
      for (String cmd : commands) {
         String finalCmd = cmd.replace("{player}", name).replace("{uuid}", uuid.toString());
         // Command dispatch must happen on the main server thread.
         Bukkit.getScheduler().runTask(this.plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd));
      }
   }
}
