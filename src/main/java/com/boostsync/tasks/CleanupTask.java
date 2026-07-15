package com.boostsync.tasks;

import com.boostsync.BoostSync;
import org.bukkit.scheduler.BukkitRunnable;

/** Periodically drops verification codes that were never used before expiring. */
public class CleanupTask extends BukkitRunnable {
   private final BoostSync plugin;

   public CleanupTask(BoostSync plugin) {
      this.plugin = plugin;
   }

   @Override
   public void run() {
      try {
         if (this.plugin.getVerificationManager() != null) {
            int removed = this.plugin.getVerificationManager().cleanupExpiredCodes();
            if (removed > 0) {
               this.plugin.getLogger().fine("Cleaned up " + removed + " expired verification codes");
            }
         }
      } catch (Exception e) {
         this.plugin.getLogger().warning("Error during cleanup task: " + e.getMessage());
      }
   }
}
