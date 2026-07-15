package com.boostsync.commands;

import com.boostsync.BoostSync;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class BoostSyncCommand implements CommandExecutor, TabCompleter {
   private final BoostSync plugin;

   public BoostSyncCommand(BoostSync plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("boostsync.admin")) {
         this.plugin.getMessageUtil().sendMessage(sender, "no-permission");
         return true;
      }

      if (args.length == 0) {
         this.sendHelp(sender);
         return true;
      }

      switch (args[0].toLowerCase()) {
         case "reload":
            this.handleReload(sender);
            break;
         case "help":
            this.sendHelp(sender);
            break;
         default:
            sender.sendMessage(this.plugin.getMessageUtil().colorize("&cUnknown subcommand. Use /boostsync help"));
      }
      return true;
   }

   private void handleReload(CommandSender sender) {
      if (!sender.hasPermission("boostsync.reload")) {
         this.plugin.getMessageUtil().sendMessage(sender, "no-permission");
         return;
      }
      try {
         this.plugin.reloadPlugin();
         this.plugin.getMessageUtil().sendMessage(sender, "reload-success");
      } catch (Exception e) {
         this.plugin.getMessageUtil().sendMessage(sender, "reload-failed");
         this.plugin.getLogger().severe("Failed to reload plugin: " + e.getMessage());
         e.printStackTrace();
      }
   }

   private void sendHelp(CommandSender sender) {
      String prefix = this.plugin.getMessageUtil().getPrefix();
      sender.sendMessage(this.plugin.getMessageUtil().colorize(prefix + " &eBoostSync Commands:"));
      sender.sendMessage(this.plugin.getMessageUtil().colorize("&7/booster &f- Verify your boost and claim rewards"));
      sender.sendMessage(this.plugin.getMessageUtil().colorize("&7/boostsync reload &f- Reload the configuration"));
      sender.sendMessage(this.plugin.getMessageUtil().colorize("&7/boostsync help &f- Show this help message"));
   }

   @Override
   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList<>();
      if (args.length == 1 && sender.hasPermission("boostsync.admin")) {
         completions.add("reload");
         completions.add("help");
      }
      return completions;
   }
}
