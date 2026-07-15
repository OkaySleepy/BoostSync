package com.boostsync.utils;

import com.boostsync.BoostSync;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageUtil {
   private final BoostSync plugin;
   private String prefix;
   private final ConcurrentHashMap<String, String> colorCache;

   public MessageUtil(BoostSync plugin) {
      this.plugin = plugin;
      this.colorCache = new ConcurrentHashMap<>(16, 0.75F, 2);
      this.loadPrefix();
   }

   public void reload() {
      this.colorCache.clear();
      this.loadPrefix();
   }

   private void loadPrefix() {
      String rawPrefix = this.plugin.getConfig().getString("prefix", "&7[&#5865F2Boost&7]");
      this.prefix = this.colorize(rawPrefix);
   }

   public void sendMessage(Player player, String key) {
      this.sendMessage((CommandSender) player, key, null);
   }

   public void sendMessage(Player player, String key, String code) {
      this.sendMessage((CommandSender) player, key, code);
   }

   public void sendMessage(CommandSender sender, String key) {
      this.sendMessage(sender, key, null);
   }

   public void sendMessage(CommandSender sender, String key, String code) {
      String rawMessage = this.plugin.getConfig().getString("messages." + key, "");
      if (!rawMessage.isEmpty()) {
         StringBuilder messageBuilder = new StringBuilder(rawMessage.length() + 50);
         messageBuilder.append(rawMessage);
         this.replaceAll(messageBuilder, "{prefix}", this.prefix);
         this.replaceAll(messageBuilder, "{days}", String.valueOf(this.plugin.getConfig().getInt("rank.duration-days", 7)));
         if (sender instanceof Player) {
            this.replaceAll(messageBuilder, "{player}", sender.getName());
         }
         if (code != null) {
            this.replaceAll(messageBuilder, "{code}", code);
         }
         sender.sendMessage(this.colorize(messageBuilder.toString()));
      }
   }

   private void replaceAll(StringBuilder sb, String target, String replacement) {
      int index = sb.indexOf(target);
      while (index != -1) {
         sb.replace(index, index + target.length(), replacement);
         index = sb.indexOf(target, index + replacement.length());
      }
   }

   public String colorize(String message) {
      if (message == null) {
         return "";
      }
      String cached = this.colorCache.get(message);
      if (cached != null) {
         return cached;
      }

      String translated = message;
      // Parse hex colors (&#RRGGBB)
      java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("&#[a-fA-F0-9]{6}");
      java.util.regex.Matcher matcher = pattern.matcher(translated);
      while (matcher.find()) {
         String hexCode = translated.substring(matcher.start() + 1, matcher.end());
         String replaceSharp = hexCode.replace('#', 'x');
         char[] ch = replaceSharp.toCharArray();
         StringBuilder builder = new StringBuilder();
         for (char c : ch) {
            builder.append("&").append(c);
         }
         translated = translated.replace("&#" + hexCode.substring(1), builder.toString());
         matcher = pattern.matcher(translated);
      }

      translated = ChatColor.translateAlternateColorCodes('&', translated);
      if (this.colorCache.size() < 100) {
         this.colorCache.put(message, translated);
      }
      return translated;
   }

   public String getPrefix() {
      return this.prefix;
   }
}
