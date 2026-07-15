package com.boostsync.manager;

import com.boostsync.BoostSync;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles the first-time link between a Minecraft account and a Discord account
 * via a short one-time code that the player pastes into the verification channel.
 */
public class VerificationManager {
   private final BoostSync plugin;
   private final ConcurrentHashMap<UUID, VerificationData> pendingVerifications;
   private final ConcurrentHashMap<String, UUID> codeToPlayer;
   private static final SecureRandom RANDOM = new SecureRandom();
   private static final char[] CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
   private static final int CODE_LENGTH = 6;

   public VerificationManager(BoostSync plugin) {
      this.plugin = plugin;
      this.pendingVerifications = new ConcurrentHashMap<>(16, 0.75F, 2);
      this.codeToPlayer = new ConcurrentHashMap<>(16, 0.75F, 2);
   }

   public String generateVerificationCode(UUID playerUUID, String playerName) {
      String code = this.generateRandomCode();
      while (this.codeToPlayer.containsKey(code)) {
         code = this.generateRandomCode();
      }

      long expiryTime = System.currentTimeMillis() + this.plugin.getConfig().getLong("verification.code-expiry", 300L) * 1000L;
      VerificationData data = new VerificationData(playerUUID, playerName, code, expiryTime);
      this.pendingVerifications.put(playerUUID, data);
      this.codeToPlayer.put(code, playerUUID);
      return code;
   }

   private String generateRandomCode() {
      char[] code = new char[CODE_LENGTH];
      for (int i = 0; i < CODE_LENGTH; i++) {
         code[i] = CODE_CHARS[RANDOM.nextInt(CODE_CHARS.length)];
      }
      return new String(code);
   }

   public boolean hasPendingVerification(UUID playerUUID) {
      return this.pendingVerifications.containsKey(playerUUID);
   }

   public String getVerificationCode(UUID playerUUID) {
      VerificationData data = this.pendingVerifications.get(playerUUID);
      return data != null ? data.code : null;
   }

   public UUID getPlayerUUID(String code) {
      return this.codeToPlayer.get(code);
   }

   public String getPlayerName(UUID playerUUID) {
      VerificationData data = this.pendingVerifications.get(playerUUID);
      return data != null ? data.playerName : null;
   }

   public boolean isCodeExpired(String code) {
      UUID playerUUID = this.codeToPlayer.get(code);
      if (playerUUID == null) {
         return true;
      }
      VerificationData data = this.pendingVerifications.get(playerUUID);
      return data == null || System.currentTimeMillis() > data.expiryTime;
   }

   public void removeVerification(UUID playerUUID) {
      VerificationData data = this.pendingVerifications.remove(playerUUID);
      if (data != null) {
         this.codeToPlayer.remove(data.code);
      }
   }

   public int cleanupExpiredCodes() {
      long currentTime = System.currentTimeMillis();
      AtomicInteger removed = new AtomicInteger();
      this.pendingVerifications.entrySet().removeIf(entry -> {
         VerificationData data = entry.getValue();
         if (currentTime > data.expiryTime) {
            this.codeToPlayer.remove(data.code);
            removed.incrementAndGet();
            return true;
         }
         return false;
      });
      return removed.get();
   }

   public boolean linkDiscordId(String code, String discordId) {
      UUID playerUUID = this.codeToPlayer.get(code);
      if (playerUUID != null) {
         VerificationData data = this.pendingVerifications.get(playerUUID);
         if (data != null) {
            if (data.discordId != null && !data.discordId.equals(discordId)) {
               return false;
            }
            data.discordId = discordId;
            return true;
         }
      }
      return false;
   }

   private static class VerificationData {
      final UUID playerUUID;
      final String playerName;
      final String code;
      final long expiryTime;
      volatile String discordId;

      VerificationData(UUID playerUUID, String playerName, String code, long expiryTime) {
         this.playerUUID = playerUUID;
         this.playerName = playerName;
         this.code = code;
         this.expiryTime = expiryTime;
         this.discordId = null;
      }
   }
}
