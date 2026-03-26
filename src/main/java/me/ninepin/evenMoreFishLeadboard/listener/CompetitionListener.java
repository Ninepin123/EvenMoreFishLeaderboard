package me.ninepin.evenMoreFishLeadboard.listener;

import com.oheers.fish.EvenMoreFish;
import com.oheers.fish.api.EMFCompetitionEndEvent;
import com.oheers.fish.api.EMFCompetitionStartEvent;
import com.oheers.fish.api.events.EMFPluginReloadEvent;
import com.oheers.fish.competition.configs.CompetitionFile;
import com.oheers.fish.utils.TimeCode;
import me.ninepin.evenMoreFishLeadboard.EvenMoreFishLeadboard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CompetitionListener implements Listener {

    private final EvenMoreFishLeadboard plugin;
    private int lastCompetitionCount = -1;

    public CompetitionListener(EvenMoreFishLeadboard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onCompetitionStart(EMFCompetitionStartEvent event) {
        plugin.getLogger().info("競賽開始: " + event.getCompetition().getCompetitionName());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onCompetitionEnd(EMFCompetitionEndEvent event) {
        plugin.getLogger().info("競賽結束: " + event.getCompetition().getCompetitionName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEmfReload(EMFPluginReloadEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            int newCount = getCompetitionCount();
            if (newCount != lastCompetitionCount) {
                plugin.getLogger().info("偵測到比賽數量變動: " + lastCompetitionCount + " → " + newCount + "，自動重載排行榜全息圖");
                lastCompetitionCount = newCount;
                plugin.reloadConfig();
                plugin.getHologramManager().removeAllHolograms();
                plugin.getHologramManager().showToAll();
            } else {
                plugin.getLogger().info("EvenMoreFish 已重載，比賽數量未變動 (" + newCount + ")");
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getHologramManager().showToPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getHologramManager().removePlayerHologram(event.getPlayer());
    }

    private int getCompetitionCount() {
        try {
            Map<TimeCode, CompetitionFile> competitions =
                    EvenMoreFish.getInstance().getCompetitionQueue().getCompetitions();
            if (competitions == null) return 0;
            Set<String> uniqueIds = competitions.values().stream()
                    .map(CompetitionFile::getId)
                    .collect(Collectors.toSet());
            return uniqueIds.size();
        } catch (Exception e) {
            return 0;
        }
    }
}
