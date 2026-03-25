package me.ninepin.evenMoreFishLeadboard.hologram;

import com.oheers.fish.EvenMoreFish;
import com.oheers.fish.competition.Competition;
import com.oheers.fish.competition.CompetitionEntry;
import com.oheers.fish.competition.CompetitionType;
import com.oheers.fish.competition.configs.CompetitionFile;
import com.oheers.fish.utils.TimeCode;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import me.ninepin.evenMoreFishLeadboard.EvenMoreFishLeadboard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {

    private final EvenMoreFishLeadboard plugin;
    private final Map<UUID, String> playerHolograms;
    private final Map<UUID, List<CompetitionInfo>> playerCompetitionList;
    private final Map<UUID, Integer> playerCurrentIndex;

    public HologramManager(EvenMoreFishLeadboard plugin) {
        this.plugin = plugin;
        this.playerHolograms = new ConcurrentHashMap<>();
        this.playerCompetitionList = new ConcurrentHashMap<>();
        this.playerCurrentIndex = new ConcurrentHashMap<>();
    }

    public static class CompetitionInfo {
        private final String id;
        private final String name;
        private final CompetitionType type;
        private final int duration;

        public CompetitionInfo(String id, String name, CompetitionType type, int duration) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.duration = duration;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public CompetitionType getType() { return type; }
        public int getDuration() { return duration; }
    }

    private Location getHologramLocation() {
        return plugin.getHologramLocation();
    }

    /**
     * 顯示全息圖給玩家（預設顯示主選單）
     */
    public void showToPlayer(Player player) {
        Location hologramLocation = getHologramLocation();
        if (hologramLocation == null) return;

        removePlayerHologram(player);

        List<CompetitionInfo> competitions = getAllCompetitions();
        playerCompetitionList.put(player.getUniqueId(), competitions);

        if (competitions.isEmpty()) return;

        String hologramName = getHologramName(player);
        List<String> lines = buildMenuLines(competitions);
        Hologram hologram = DHAPI.createHologram(hologramName, hologramLocation, false, lines);
        hologram.show(player, 0);
        playerHolograms.put(player.getUniqueId(), hologramName);
        playerCurrentIndex.put(player.getUniqueId(), -1);
    }

    /**
     * 顯示全息圖給所有線上玩家
     */
    public void showToAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            showToPlayer(player);
        }
    }

    /**
     * 點擊全息圖後切換到下一個比賽（循環）
     */
    public void cycleToNext(Player player) {
        List<CompetitionInfo> competitions = playerCompetitionList.get(player.getUniqueId());
        if (competitions == null || competitions.isEmpty()) return;

        int currentIndex = playerCurrentIndex.getOrDefault(player.getUniqueId(), -1);
        int totalCompetitions = competitions.size();

        int nextIndex;
        if (currentIndex < totalCompetitions - 1) {
            nextIndex = currentIndex + 1;
        } else {
            nextIndex = -1;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            showAtIndex(player, nextIndex);
        }, 1L);
    }

    private void showAtIndex(Player player, int index) {
        List<CompetitionInfo> competitions = playerCompetitionList.get(player.getUniqueId());
        if (competitions == null || competitions.isEmpty()) return;

        Location hologramLocation = getHologramLocation();
        if (hologramLocation == null) return;

        removePlayerHologramKeepList(player);
        playerCompetitionList.put(player.getUniqueId(), competitions);

        String hologramName = getHologramName(player);

        List<String> lines;
        if (index == -1) {
            lines = buildMenuLines(competitions);
        } else if (index >= 0 && index < competitions.size()) {
            lines = buildCompetitionLines(competitions.get(index));
        } else {
            return;
        }

        Hologram hologram = DHAPI.createHologram(hologramName, hologramLocation, false, lines);
        hologram.show(player, 0);
        playerHolograms.put(player.getUniqueId(), hologramName);
        playerCurrentIndex.put(player.getUniqueId(), index);
    }

    private List<String> buildMenuLines(List<CompetitionInfo> competitions) {
        List<String> lines = new ArrayList<>();

        lines.add(ChatColor.GOLD + "=== " + ChatColor.YELLOW + "競賽列表" + ChatColor.GOLD + " ===");
        lines.add(ChatColor.GRAY + "點擊全息圖切換比賽");
        lines.add(ChatColor.GRAY + "------------------------");

        for (int i = 0; i < competitions.size(); i++) {
            CompetitionInfo info = competitions.get(i);
            String statusIcon = getCompetitionStatusIcon(info.getId());
            lines.add(ChatColor.WHITE + String.valueOf(i + 1) + ". " + statusIcon + " " +
                    ChatColor.YELLOW + info.getName() +
                    ChatColor.DARK_GRAY + " [" + formatCompetitionType(info.getType()) + "]");
        }

        lines.add(ChatColor.GRAY + "------------------------");

        return lines;
    }

    private List<String> buildCompetitionLines(CompetitionInfo info) {
        List<String> lines = new ArrayList<>();

        Competition activeCompetition = Competition.getCurrentlyActive();
        boolean isActive = activeCompetition != null &&
                activeCompetition.getCompetitionFile() != null &&
                activeCompetition.getCompetitionFile().getId().equals(info.getId());

        lines.add(ChatColor.GOLD + "=== " + ChatColor.YELLOW + info.getName() + ChatColor.GOLD + " ===");
        lines.add(ChatColor.AQUA + "類型: " + ChatColor.WHITE + formatCompetitionType(info.getType()));

        if (isActive && activeCompetition != null) {
            lines.add(ChatColor.GREEN + "狀態: " + ChatColor.BOLD + "進行中");
            lines.add(ChatColor.AQUA + "剩餘時間: " + ChatColor.WHITE + formatTime(activeCompetition.getTimeLeft()));
            lines.add(ChatColor.GRAY + "------------------------");

            List<CompetitionEntry> entries = activeCompetition.getLeaderboard().getEntries();

            if (entries.isEmpty()) {
                lines.add(ChatColor.RED + "暫無參賽者");
            } else {
                int displayCount = Math.min(entries.size(), 10);
                for (int i = 0; i < displayCount; i++) {
                    CompetitionEntry entry = entries.get(i);
                    String playerName = getPlayerName(entry.getPlayer());
                    String value = formatValue(entry, info.getType());
                    ChatColor rankColor = getRankColor(i + 1);
                    lines.add(rankColor + "#" + (i + 1) + " " + ChatColor.WHITE + playerName +
                            ChatColor.GRAY + " - " + ChatColor.YELLOW + value);
                }
                if (entries.size() > 10) {
                    lines.add(ChatColor.GRAY + "... 還有 " + (entries.size() - 10) + " 名參賽者");
                }
            }
        } else {
            lines.add(ChatColor.RED + "狀態: " + ChatColor.BOLD + "未進行");
            lines.add(ChatColor.GRAY + "------------------------");
            lines.add(ChatColor.YELLOW + "此比賽目前未進行中");
            lines.add(ChatColor.YELLOW + "請等待比賽開始");
        }

        lines.add(ChatColor.GRAY + "------------------------");
        lines.add(ChatColor.DARK_GRAY + "點擊切換下一個");

        return lines;
    }

    private List<CompetitionInfo> getAllCompetitions() {
        List<CompetitionInfo> list = new ArrayList<>();

        try {
            Map<TimeCode, CompetitionFile> competitions =
                    EvenMoreFish.getInstance().getCompetitionQueue().getCompetitions();

            if (competitions != null) {
                Set<String> addedIds = new HashSet<>();
                for (CompetitionFile file : competitions.values()) {
                    String id = file.getId();
                    if (!addedIds.contains(id)) {
                        list.add(new CompetitionInfo(id, id, file.getType(), file.getDuration()));
                        addedIds.add(id);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("取得比賽列表時發生錯誤: " + e.getMessage());
        }

        return list;
    }

    private String getCompetitionStatusIcon(String competitionId) {
        Competition active = Competition.getCurrentlyActive();
        if (active != null && active.getCompetitionFile() != null &&
                active.getCompetitionFile().getId().equals(competitionId)) {
            return ChatColor.GREEN + "●";
        }
        return ChatColor.GRAY + "○";
    }

    public void updatePlayerHologram(Player player) {
        String hologramName = playerHolograms.get(player.getUniqueId());
        if (hologramName == null) return;

        Integer currentIndex = playerCurrentIndex.get(player.getUniqueId());
        if (currentIndex == null) return;

        Hologram hologram = DHAPI.getHologram(hologramName);
        if (hologram == null) return;

        List<CompetitionInfo> competitions = playerCompetitionList.get(player.getUniqueId());
        if (competitions == null || competitions.isEmpty()) return;

        List<String> lines;
        if (currentIndex == -1) {
            lines = buildMenuLines(competitions);
        } else if (currentIndex >= 0 && currentIndex < competitions.size()) {
            lines = buildCompetitionLines(competitions.get(currentIndex));
        } else {
            return;
        }

        DHAPI.setHologramLines(hologram, lines);
    }

    private void removePlayerHologramKeepList(Player player) {
        String hologramName = playerHolograms.remove(player.getUniqueId());
        if (hologramName != null) {
            DHAPI.removeHologram(hologramName);
        }
        playerCurrentIndex.remove(player.getUniqueId());
    }

    public void removePlayerHologram(Player player) {
        String hologramName = playerHolograms.remove(player.getUniqueId());
        if (hologramName != null) {
            DHAPI.removeHologram(hologramName);
        }
        playerCurrentIndex.remove(player.getUniqueId());
        playerCompetitionList.remove(player.getUniqueId());
    }

    public void removeAllHolograms() {
        for (String name : playerHolograms.values()) {
            DHAPI.removeHologram(name);
        }
        playerHolograms.clear();
        playerCurrentIndex.clear();
        playerCompetitionList.clear();
    }

    private String getHologramName(Player player) {
        return "emf_lb_" + player.getUniqueId().toString().substring(0, 8);
    }

    private String formatCompetitionType(CompetitionType type) {
        return switch (type) {
            case LARGEST_FISH -> "最大魚";
            case SPECIFIC_FISH -> "特定魚種";
            case MOST_FISH -> "最多魚";
            case SPECIFIC_RARITY -> "特定稀有度";
            case LARGEST_TOTAL -> "最大總重";
            case RANDOM -> "隨機";
            case SHORTEST_FISH -> "最短魚";
            case SHORTEST_TOTAL -> "最短總重";
        };
    }

    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    private ChatColor getRankColor(int rank) {
        return switch (rank) {
            case 1 -> ChatColor.GOLD;
            case 2 -> ChatColor.WHITE;
            case 3 -> ChatColor.YELLOW;
            default -> ChatColor.GRAY;
        };
    }

    private String formatValue(CompetitionEntry entry, CompetitionType type) {
        float value = entry.getValue();
        return switch (type) {
            case LARGEST_FISH, SHORTEST_FISH, LARGEST_TOTAL, SHORTEST_TOTAL ->
                    String.format("%.2f 公分", value);
            case MOST_FISH, SPECIFIC_FISH, SPECIFIC_RARITY ->
                    String.format("%.0f 條", value);
            default -> String.format("%.2f", value);
        };
    }

    private String getPlayerName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }
        return "玩家_" + uuid.toString().substring(0, 8);
    }
}
