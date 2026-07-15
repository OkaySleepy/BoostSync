package com.boostsync.discord;

import com.boostsync.BoostSync;
import com.boostsync.manager.VerificationManager;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.bukkit.Bukkit;

public class DiscordBot extends ListenerAdapter {
   private final BoostSync plugin;
   private final String token;
   private JDA jda;
   private volatile Guild guild;
   private volatile Role boosterRole;
   private volatile long lastCacheRefresh;

   public DiscordBot(BoostSync plugin, String token) {
      this.plugin = plugin;
      this.token = token;
      this.lastCacheRefresh = 0L;
   }

   public boolean start() {
      try {
         this.jda = JDABuilder.createDefault(this.token)
            .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.DIRECT_MESSAGES)
            .disableCache(
               CacheFlag.ACTIVITY,
               CacheFlag.VOICE_STATE,
               CacheFlag.EMOJI,
               CacheFlag.STICKER,
               CacheFlag.CLIENT_STATUS,
               CacheFlag.ONLINE_STATUS,
               CacheFlag.SCHEDULED_EVENTS
            )
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .setLargeThreshold(250)
            .addEventListeners(this)
            .build();
         this.jda.awaitReady();
         this.refreshCache();
         this.schedulePeriodicCacheRefresh();
         this.plugin.getLogger().info("Discord bot connected successfully!");
         return true;
      } catch (Exception e) {
         this.plugin.getLogger().severe("Failed to start Discord bot: " + e.getMessage());
         e.printStackTrace();
         return false;
      }
   }

   private void refreshCache() {
      try {
         String guildId = this.plugin.getConfig().getString("discord.guild-id", "");
         if (!guildId.isEmpty() && !guildId.equals("YOUR_GUILD_ID_HERE")) {
            this.guild = this.jda.getGuildById(guildId);
            if (this.guild != null) {
               String boosterRoleId = this.plugin.getConfig().getString("discord.booster-role-id", "");
               if (!boosterRoleId.isEmpty() && !boosterRoleId.equals("YOUR_BOOSTER_ROLE_ID_HERE")) {
                  this.boosterRole = this.guild.getRoleById(boosterRoleId);
               }
            }
         }
         this.lastCacheRefresh = System.currentTimeMillis();
      } catch (Exception e) {
         this.plugin.getLogger().warning("Failed to refresh Discord cache: " + e.getMessage());
      }
   }

   private void schedulePeriodicCacheRefresh() {
      int refreshInterval = this.plugin.getConfig().getInt("discord.cache.refresh-interval", 300);
      Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, this::refreshCache, refreshInterval * 20L, refreshInterval * 20L);
   }

   public void shutdown() {
      if (this.jda != null) {
         this.jda.shutdown();
         try {
            if (!this.jda.awaitShutdown(5L, TimeUnit.SECONDS)) {
               this.jda.shutdownNow();
            }
         } catch (InterruptedException e) {
            this.jda.shutdownNow();
            Thread.currentThread().interrupt();
         }
      }
   }

   @Override
   public void onMessageReceived(MessageReceivedEvent event) {
      if (event.getAuthor().isBot()) {
         return;
      }
      String requiredChannelId = this.plugin.getConfig().getString("discord.verification-channel-id", "");
      if (!requiredChannelId.isEmpty() && !requiredChannelId.equals("YOUR_CHANNEL_ID_HERE") && !event.getChannel().getId().equals(requiredChannelId)) {
         return;
      }

      String message = event.getMessage().getContentRaw().trim();
      if (message.length() == 6 && message.matches("[A-Z0-9]{6}")) {
         User user = event.getAuthor();
         CompletableFuture.runAsync(() -> this.handleVerification(event, message, user));
      }
   }

   private void handleVerification(MessageReceivedEvent event, String code, User discordUser) {
      VerificationManager verificationManager = this.plugin.getVerificationManager();
      UUID playerUUID = verificationManager.getPlayerUUID(code);
      if (playerUUID == null) {
         event.getMessage().reply("Invalid verification code. Please generate a new code in-game.").queue();
      } else if (!verificationManager.linkDiscordId(code, discordUser.getId())) {
         event.getMessage().reply("This code has already been claimed by another Discord user!").queue();
      } else if (verificationManager.isCodeExpired(code)) {
         verificationManager.removeVerification(playerUUID);
         event.getMessage().reply("This verification code has expired. Please generate a new one in-game.").queue();
      } else if (this.guild != null && this.boosterRole != null) {
         Member member = this.guild.getMemberById(discordUser.getId());
         if (member == null) {
            this.plugin.getLogger().warning("Could not find member in guild: " + discordUser.getId() + " (they might have left the server)");
            event.getMessage().reply("You are not in the Discord server!").queue();
         } else if (!member.getRoles().contains(this.boosterRole)) {
            this.plugin.getLogger().info("User " + discordUser.getName() + " (" + discordUser.getId() + ") doesn't have the booster role");
            event.getMessage().reply("You don't have the booster role! Boost the server to claim rewards.").queue();
         } else {
            String playerName = verificationManager.getPlayerName(playerUUID);
            String discordId = discordUser.getId();
            EmbedBuilder embed = new EmbedBuilder()
               .setColor(0x5865F2)
               .setTitle("✅ Verification Successful!")
               .setDescription("Welcome, **" + playerName + "**! Your Minecraft account has been successfully linked.")
               .addField("Minecraft Username", playerName, true)
               .addField("Discord User", discordUser.getAsMention(), true)
               .setThumbnail("https://mc-heads.net/avatar/" + playerUUID + "/100")
               .setFooter("BoostSync • Rewards have been sent in-game", null)
               .setTimestamp(Instant.now());
            event.getMessage().replyEmbeds(embed.build()).queue(reply -> event.getMessage()
               .delete()
               .queueAfter(
                  3L,
                  TimeUnit.SECONDS,
                  success -> this.plugin.getLogger().info("Deleted verification message from " + discordUser.getName()),
                  error -> this.plugin.getLogger().warning("Could not delete message: " + error.getMessage())
               ));

            verificationManager.removeVerification(playerUUID);
            Bukkit.getScheduler().runTask(this.plugin, () -> {
               boolean gaveRewards = this.plugin.getRewardManager().applyBoost(playerUUID, playerName, discordId, false);
               org.bukkit.entity.Player player = Bukkit.getPlayer(playerUUID);
               if (player != null && player.isOnline()) {
                  this.plugin.getMessageUtil().sendMessage(player, gaveRewards ? "boost-rewards" : "boost-renewed");
               }
            });
         }
      } else {
         this.plugin.getLogger().warning("Guild or booster role not cached! Guild: " + (this.guild != null) + ", Role: " + (this.boosterRole != null));
         event.getMessage().reply("Bot configuration error. Please contact an administrator.").queue();
      }
   }

   public boolean hasBoosterRole(String discordUserId) {
      if (this.guild != null && this.boosterRole != null) {
         Member member = this.guild.getMemberById(discordUserId);
         return member != null && member.getRoles().contains(this.boosterRole);
      }
      return false;
   }
}
