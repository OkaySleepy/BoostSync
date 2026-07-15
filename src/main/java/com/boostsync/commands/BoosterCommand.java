package com.boostsync.commands;

import com.boostsync.BoostSync;
import com.boostsync.manager.DataManager;
import com.boostsync.manager.RewardManager;
import com.boostsync.manager.VerificationManager;
import com.boostsync.utils.MessageUtil;
import java.util.UUID;
import java.util.WeakHashMap;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BoosterCommand implements CommandExecutor {
   private final BoostSync plugin;
   private final VerificationManager verificationManager;
   private final DataManager dataManager;
   private final RewardManager rewardManager;
   private final MessageUtil messageUtil;
   private final WeakHashMap<UUID, Long> cooldowns;

   public BoosterCommand(BoostSync plugin) {
      this.plugin = plugin;
      this.verificationManager = plugin.getVerificationManager();
      this.dataManager = plugin.getDataManager();
      this.rewardManager = plugin.getRewardManager();
      this.messageUtil = plugin.getMessageUtil();
      this.cooldowns = new WeakHashMap<>();
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage("This command can only be used by players!");
         return true;
      }

      if (!player.hasPermission("boostsync.use")) {
         this.messageUtil.sendMessage(player, "no-permission");
         return true;
      }

      if (this.plugin.getDiscordBot() == null) {
         this.messageUtil.sendMessage(player, "bot-offline");
         return true;
      }

      UUID uuid = player.getUniqueId();
      boolean hasBypass = player.hasPermission("boostsync.bypass");
      boolean hasBypassCooldown = player.hasPermission("boostsync.bypass.cooldown");
      boolean hasBypassOneTime = player.hasPermission("boostsync.bypass.onetime");
      if (hasBypass) {
         this.messageUtil.sendMessage(player, "bypass-used");
      }

      if (!hasBypass && !hasBypassCooldown && this.onCooldown(uuid)) {
         this.messageUtil.sendMessage(player, "cooldown");
         return true;
      }

      // Already linked? Re-check their boost status straight away instead of
      // making them go through Discord again.
      if (this.dataManager.isLinked(uuid)) {
         this.recheckBoost(player, uuid, hasBypass || hasBypassOneTime);
         this.cooldowns.put(uuid, System.currentTimeMillis());
         return true;
      }

      // First time: hand out a verification code to link their Discord account.
      this.startVerification(player, uuid);
      return true;
   }

   private void recheckBoost(Player player, UUID uuid, boolean forceRewards) {
      String discordId = this.dataManager.getDiscordId(uuid);
      String name = player.getName();
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
         boolean boosting = this.plugin.getDiscordBot().hasBoosterRole(discordId);
         Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (!player.isOnline()) {
               return;
            }
            if (boosting) {
               boolean gaveRewards = this.rewardManager.applyBoost(uuid, name, discordId, forceRewards);
               this.messageUtil.sendMessage(player, gaveRewards ? "boost-rewards" : "boost-renewed");
            } else {
               this.messageUtil.sendMessage(player, "not-boosting");
            }
         });
      });
   }

   private void startVerification(Player player, UUID uuid) {
      if (this.verificationManager.hasPendingVerification(uuid)) {
         String code = this.verificationManager.getVerificationCode(uuid);
         this.messageUtil.sendMessage(player, "verification-pending");
         this.messageUtil.sendMessage(player, "code-generated", code);
         return;
      }

      String code = this.verificationManager.generateVerificationCode(uuid, player.getName());
      this.messageUtil.sendMessage(player, "code-generated", code);

      TextComponent message = new TextComponent("[");
      message.setColor(ChatColor.GRAY);
      TextComponent clickable = new TextComponent(" CLICK TO COPY ");
      clickable.setColor(ChatColor.GREEN);
      clickable.setBold(true);
      clickable.setClickEvent(new ClickEvent(Action.COPY_TO_CLIPBOARD, code));
      clickable.setHoverEvent(new HoverEvent(
         HoverEvent.Action.SHOW_TEXT,
         new ComponentBuilder("Click to copy: " + code).color(ChatColor.YELLOW).create()
      ));
      TextComponent close = new TextComponent("]");
      close.setColor(ChatColor.GRAY);
      message.addExtra(clickable);
      message.addExtra(close);
      player.spigot().sendMessage(message);

      this.messageUtil.sendMessage(player, "code-instructions");
      this.cooldowns.put(uuid, System.currentTimeMillis());
   }

   private boolean onCooldown(UUID uuid) {
      Long lastUsed = this.cooldowns.get(uuid);
      if (lastUsed == null) {
         return false;
      }
      long cooldownTime = this.plugin.getConfig().getLong("verification.command-cooldown", 60L) * 1000L;
      return lastUsed + cooldownTime - System.currentTimeMillis() > 0L;
   }
}
