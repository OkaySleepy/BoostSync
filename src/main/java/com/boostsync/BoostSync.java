package com.boostsync;

import com.boostsync.commands.BoostSyncCommand;
import com.boostsync.commands.BoosterCommand;
import com.boostsync.discord.DiscordBot;
import com.boostsync.manager.DataManager;
import com.boostsync.manager.RewardManager;
import com.boostsync.manager.VerificationManager;
import com.boostsync.tasks.CleanupTask;
import com.boostsync.tasks.RankTask;
import com.boostsync.utils.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class BoostSync extends JavaPlugin {
   private static BoostSync instance;
   private DiscordBot discordBot;
   private VerificationManager verificationManager;
   private DataManager dataManager;
   private RewardManager rewardManager;
   private MessageUtil messageUtil;
   private BukkitTask cleanupTask;
   private BukkitTask rankTask;
   private BukkitTask autoSaveTask;

   @Override
   public void onEnable() {
      instance = this;
      this.saveDefaultConfig();
      this.messageUtil = new MessageUtil(this);
      this.dataManager = new DataManager(this);
      this.verificationManager = new VerificationManager(this);
      this.rewardManager = new RewardManager(this);

      this.startDiscordBot();

      this.getCommand("booster").setExecutor(new BoosterCommand(this));
      BoostSyncCommand adminCommand = new BoostSyncCommand(this);
      this.getCommand("boostsync").setExecutor(adminCommand);
      this.getCommand("boostsync").setTabCompleter(adminCommand);

      this.startCleanupTask();
      this.startRankTask();
      if (this.getConfig().getInt("database.auto-save-interval", 300) > 0) {
         this.startAutoSaveTask();
      }

      this.getLogger().info("BoostSync has been enabled successfully!");
   }

   @Override
   public void onDisable() {
      cancel(this.cleanupTask);
      cancel(this.rankTask);
      cancel(this.autoSaveTask);

      if (this.discordBot != null) {
         this.discordBot.shutdown();
      }

      if (this.dataManager != null) {
         this.dataManager.save();
      }

      this.verificationManager = null;
      this.rewardManager = null;
      this.dataManager = null;
      this.messageUtil = null;
      this.discordBot = null;
      this.getLogger().info("BoostSync has been disabled.");
   }

   private void startDiscordBot() {
      String token = this.getConfig().getString("discord.token", "");
      if (!token.isEmpty() && !token.equals("YOUR_BOT_TOKEN_HERE")) {
         this.getServer().getScheduler().runTaskAsynchronously(this, () -> {
            this.discordBot = new DiscordBot(this, token);
            if (!this.discordBot.start()) {
               this.getLogger().severe("Failed to start Discord bot! Please check your token.");
            }
         });
      } else {
         this.getLogger().warning("Discord bot token is not set in config.yml!");
         this.getLogger().warning("Discord verification features will be unavailable.");
         this.getLogger().warning("Please set your bot token and reload the plugin to enable them.");
      }
   }

   private void startCleanupTask() {
      int interval = this.getConfig().getInt("verification.cleanup-interval", 120) * 20;
      this.cleanupTask = new CleanupTask(this).runTaskTimerAsynchronously(this, interval, interval);
   }

   private void startRankTask() {
      long interval = this.getConfig().getLong("rank.check-interval-minutes", 30L) * 60L * 20L;
      this.rankTask = new RankTask(this).runTaskTimerAsynchronously(this, interval, interval);
   }

   private void startAutoSaveTask() {
      int interval = this.getConfig().getInt("database.auto-save-interval", 300) * 20;
      this.autoSaveTask = this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
         if (this.dataManager != null) {
            this.dataManager.save();
         }
      }, interval, interval);
   }

   public void reloadPlugin() {
      cancel(this.cleanupTask);
      cancel(this.rankTask);
      cancel(this.autoSaveTask);

      this.reloadConfig();
      this.messageUtil.reload();
      this.dataManager.reload();
      if (this.discordBot != null) {
         this.discordBot.shutdown();
         this.discordBot = null;
      }

      this.startDiscordBot();
      this.startCleanupTask();
      this.startRankTask();
      if (this.getConfig().getInt("database.auto-save-interval", 300) > 0) {
         this.startAutoSaveTask();
      }
   }

   private static void cancel(BukkitTask task) {
      if (task != null && !task.isCancelled()) {
         task.cancel();
      }
   }

   public static BoostSync getInstance() {
      return instance;
   }

   public DiscordBot getDiscordBot() {
      return this.discordBot;
   }

   public VerificationManager getVerificationManager() {
      return this.verificationManager;
   }

   public DataManager getDataManager() {
      return this.dataManager;
   }

   public RewardManager getRewardManager() {
      return this.rewardManager;
   }

   public MessageUtil getMessageUtil() {
      return this.messageUtil;
   }
}
