package me.ninepin.evenMoreFishLeadboard.listener;

import eu.decentsoftware.holograms.event.HologramClickEvent;
import me.ninepin.evenMoreFishLeadboard.hologram.HologramManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * 全息圖點擊事件監聽器
 * 點擊全息圖即切換到下一個比賽
 */
public class HologramClickListener implements Listener {

    private final HologramManager hologramManager;

    public HologramClickListener(me.ninepin.evenMoreFishLeadboard.EvenMoreFishLeadboard plugin,
                                  HologramManager hologramManager) {
        this.hologramManager = hologramManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onHologramClick(HologramClickEvent event) {
        String hologramName = event.getHologram().getName();
        if (!hologramName.startsWith("emf_lb_")) {
            return;
        }

        Player player = event.getPlayer();
        hologramManager.cycleToNext(player);
    }
}
