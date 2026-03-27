package me.ninepin.evenMoreFishLeadboard.hologram;

import me.ninepin.evenMoreFishLeadboard.EvenMoreFishLeadboard;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 全息圖自動更新任務
 * 每 2 ticks 更新一次，確保倒計時與排行榜即時同步
 */
public class HologramUpdateTask extends BukkitRunnable {

    private final HologramManager hologramManager;

    public HologramUpdateTask(EvenMoreFishLeadboard plugin, HologramManager hologramManager) {
        this.hologramManager = hologramManager;
    }

    @Override
    public void run() {
        hologramManager.updateHologram();
    }
}
