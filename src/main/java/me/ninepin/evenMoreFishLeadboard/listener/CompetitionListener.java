package me.ninepin.evenMoreFishLeadboard.listener;

import com.oheers.fish.api.EMFCompetitionEndEvent;
import com.oheers.fish.api.EMFCompetitionStartEvent;
import me.ninepin.evenMoreFishLeadboard.EvenMoreFishLeadboard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CompetitionListener implements Listener {

    private final EvenMoreFishLeadboard plugin;

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

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getHologramManager().showToPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getHologramManager().removePlayerHologram(event.getPlayer());
    }
}
