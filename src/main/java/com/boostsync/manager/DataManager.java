package com.boostsync.manager;

import com.boostsync.BoostSync;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Stores everything BoostSync needs to remember about a booster:
 * their linked Discord account, the season they last claimed rewards in,
 * and when their temporary rank is due to expire.
 */
public class DataManager {
   private final BoostSync plugin;
   private File dataFile;
   private FileConfiguration dataConfig;
   private final Map<UUID, BoosterData> boosters;
   private final ReadWriteLock lock;
   private volatile boolean dirty;

   public DataManager(BoostSync plugin) {
      this.plugin = plugin;
      this.boosters = new ConcurrentHashMap<>();
      this.lock = new ReentrantReadWriteLock();
      this.dirty = false;
      this.loadData();
   }

   private void loadData() {
      this.lock.writeLock().lock();
      try {
         String fileName = this.plugin.getConfig().getString("database.file", "booster_data.yml");
         this.dataFile = new File(this.plugin.getDataFolder(), fileName);
         if (!this.dataFile.exists()) {
            try {
               this.dataFile.getParentFile().mkdirs();
               this.dataFile.createNewFile();
            } catch (IOException e) {
               this.plugin.getLogger().severe("Could not create data file: " + e.getMessage());
            }
         }

         this.dataConfig = YamlConfiguration.loadConfiguration(this.dataFile);
         this.boosters.clear();
         ConfigurationSection section = this.dataConfig.getConfigurationSection("boosters");
         if (section != null) {
            for (String uuidStr : section.getKeys(false)) {
               try {
                  UUID uuid = UUID.fromString(uuidStr);
                  ConfigurationSection entry = section.getConfigurationSection(uuidStr);
                  if (entry == null) {
                     continue;
                  }
                  BoosterData data = new BoosterData(
                     entry.getString("name", "Unknown"),
                     entry.getString("discord-id", null),
                     entry.getInt("season", -1),
                     entry.getLong("rank-expiry", 0L)
                  );
                  this.boosters.put(uuid, data);
               } catch (IllegalArgumentException e) {
                  this.plugin.getLogger().warning("Invalid UUID in data file: " + uuidStr);
               }
            }
         }

         this.dirty = false;
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   public void reload() {
      this.loadData();
   }

   public void save() {
      if (!this.dirty) {
         return;
      }
      this.lock.writeLock().lock();
      try {
         this.dataConfig.set("boosters", null);
         for (Map.Entry<UUID, BoosterData> e : this.boosters.entrySet()) {
            String path = "boosters." + e.getKey();
            BoosterData d = e.getValue();
            this.dataConfig.set(path + ".name", d.name);
            this.dataConfig.set(path + ".discord-id", d.discordId);
            this.dataConfig.set(path + ".season", d.season);
            this.dataConfig.set(path + ".rank-expiry", d.rankExpiry);
         }
         this.dataConfig.save(this.dataFile);
         this.dirty = false;
      } catch (IOException e) {
         this.plugin.getLogger().severe("Could not save data file: " + e.getMessage());
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   public void saveAsync() {
      if (this.dirty) {
         this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, this::save);
      }
   }

   /** Links (or updates the link for) a Minecraft account and its Discord account. */
   public void link(UUID uuid, String name, String discordId) {
      BoosterData data = this.boosters.computeIfAbsent(uuid, k -> new BoosterData(name, discordId, -1, 0L));
      data.name = name;
      data.discordId = discordId;
      this.dirty = true;
      this.saveAsync();
   }

   public boolean isLinked(UUID uuid) {
      BoosterData data = this.boosters.get(uuid);
      return data != null && data.discordId != null;
   }

   public String getDiscordId(UUID uuid) {
      BoosterData data = this.boosters.get(uuid);
      return data != null ? data.discordId : null;
   }

   public String getName(UUID uuid) {
      BoosterData data = this.boosters.get(uuid);
      return data != null ? data.name : null;
   }

   /** True if this player already claimed the one-time rewards for the given season. */
   public boolean hasClaimedThisSeason(UUID uuid, int season) {
      BoosterData data = this.boosters.get(uuid);
      return data != null && data.season == season;
   }

   public void setSeasonClaimed(UUID uuid, int season) {
      BoosterData data = this.boosters.get(uuid);
      if (data != null) {
         data.season = season;
         this.dirty = true;
         this.saveAsync();
      }
   }

   public long getRankExpiry(UUID uuid) {
      BoosterData data = this.boosters.get(uuid);
      return data != null ? data.rankExpiry : 0L;
   }

   public void setRankExpiry(UUID uuid, long expiry) {
      BoosterData data = this.boosters.get(uuid);
      if (data != null) {
         data.rankExpiry = expiry;
         this.dirty = true;
         this.saveAsync();
      }
   }

   /** Snapshot of every player currently tracked (used by the renewal task). */
   public Collection<UUID> getTrackedBoosters() {
      return new java.util.ArrayList<>(this.boosters.keySet());
   }

   public void clearAll() {
      this.lock.writeLock().lock();
      try {
         this.boosters.clear();
         this.dataConfig.set("boosters", null);
         this.dirty = true;
         this.save();
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   private static class BoosterData {
      volatile String name;
      volatile String discordId;
      volatile int season;
      volatile long rankExpiry;

      BoosterData(String name, String discordId, int season, long rankExpiry) {
         this.name = name;
         this.discordId = discordId;
         this.season = season;
         this.rankExpiry = rankExpiry;
      }
   }
}
