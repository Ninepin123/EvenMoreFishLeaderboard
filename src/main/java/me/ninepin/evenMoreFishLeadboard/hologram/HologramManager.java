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

/**
 * 使用 DecentHolograms 原生多頁系統管理排行榜全息圖。
 * Page 0 = 主選單（比賽列表），Page 1..N = 各比賽詳情。
 * 所有玩家共用同一個全息圖，DH 原生追蹤每位玩家的當前頁面。
 */
public class HologramManager {

    private static final String HOLOGRAM_NAME = "emf_leaderboard";

    private final EvenMoreFishLeadboard plugin;
    private List<CompetitionInfo> competitions = new ArrayList<>();

    public HologramManager(EvenMoreFishLeadboard plugin) {
        this.plugin = plugin;
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

    /**
     * 建立或完整重建全息圖（含所有頁面）
     */
    public void createOrUpdateHologram() {
        Location loc = plugin.getHologramLocation();
        if (loc == null) return;

        competitions = getAllCompetitions();

        Hologram hologram = DHAPI.getHologram(HOLOGRAM_NAME);
        if (hologram == null) {
            hologram = DHAPI.createHologram(HOLOGRAM_NAME, loc, false);
        }

        // 清除舊的額外頁面（保留 page 0）
        while (hologram.getPage(1) != null) {
            hologram.removePage(1);
        }

        if (competitions.isEmpty()) {
            // 只有一頁：無比賽提示
            DHAPI.setHologramLines(hologram, buildNoCompetitionLines());
        } else {
            // Page 0 = 主選單
            DHAPI.setHologramLines(hologram, buildMenuLines(competitions));
            // Page 1..N = 各比賽詳情
            for (CompetitionInfo info : competitions) {
                DHAPI.addHologramPage(hologram, buildCompetitionLines(info));
            }
        }
    }

    /**
     * 顯示全息圖給玩家（預設顯示主選單 page 0）
     */
    public void showToPlayer(Player player) {
        Hologram hologram = DHAPI.getHologram(HOLOGRAM_NAME);
        if (hologram == null) {
            createOrUpdateHologram();
            hologram = DHAPI.getHologram(HOLOGRAM_NAME);
        }
        if (hologram != null) {
            hologram.show(player, 0);
        }
    }

    /**
     * 重建全息圖並顯示給所有線上玩家
     */
    public void showToAll() {
        createOrUpdateHologram();
        Hologram hologram = DHAPI.getHologram(HOLOGRAM_NAME);
        if (hologram != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                hologram.show(player, 0);
            }
        }
    }

    /**
     * 點擊全息圖後切換到下一頁（循環：主選單 → 比賽1 → 比賽2 → ... → 主選單）
     */
    public void cycleToNext(Player player) {
        Hologram hologram = DHAPI.getHologram(HOLOGRAM_NAME);
        if (hologram == null) return;

        int totalPages = hologram.size();
        if (totalPages <= 1) return;

        int currentPage = hologram.getPlayerPage(player);
        int nextPage = (currentPage + 1) % totalPages;
        hologram.show(player, nextPage);
    }

    /**
     * 定時更新全息圖內容（由 HologramUpdateTask 每 2 ticks 呼叫）。
     * 若比賽數量未變，只更新各頁文字內容（不動頁面結構，避免閃爍）。
     * 若比賽數量改變，則完整重建。
     */
    public void updateHologram() {
        Hologram hologram = DHAPI.getHologram(HOLOGRAM_NAME);
        if (hologram == null) return;

        List<CompetitionInfo> newCompetitions = getAllCompetitions();
        int expectedPages = newCompetitions.isEmpty() ? 1 : newCompetitions.size() + 1;

        if (hologram.size() != expectedPages) {
            // 比賽數量變了，完整重建並把所有人重設到主選單
            competitions = newCompetitions;
            createOrUpdateHologram();
            hologram = DHAPI.getHologram(HOLOGRAM_NAME);
            if (hologram != null) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    hologram.show(p, 0);
                }
            }
            return;
        }

        competitions = newCompetitions;

        if (competitions.isEmpty()) {
            DHAPI.setHologramLines(hologram, buildNoCompetitionLines());
        } else {
            // 更新 page 0（主選單）
            DHAPI.setHologramLines(hologram, 0, buildMenuLines(competitions));
            // 更新 page 1..N（各比賽詳情）
            for (int i = 0; i < competitions.size(); i++) {
                DHAPI.setHologramLines(hologram, i + 1, buildCompetitionLines(competitions.get(i)));
            }
        }
    }

    /**
     * 移除玩家的全息圖顯示（玩家離線時呼叫）
     */
    public void removePlayerHologram(Player player) {
        Hologram hologram = DHAPI.getHologram(HOLOGRAM_NAME);
        if (hologram != null) {
            hologram.removeShowPlayer(player);
        }
    }

    /**
     * 移除整個全息圖
     */
    public void removeAllHolograms() {
        DHAPI.removeHologram(HOLOGRAM_NAME);
    }

    /**
     * 取得全息圖名稱（供 ClickListener 判斷用）
     */
    public String getHologramName() {
        return HOLOGRAM_NAME;
    }

    // ==================== 頁面內容建構 ====================

    private List<String> buildNoCompetitionLines() {
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GOLD + "=== " + ChatColor.YELLOW + "競賽列表" + ChatColor.GOLD + " ===");
        lines.add(ChatColor.GRAY + "------------------------");
        lines.add(ChatColor.RED + "暫無比賽");
        lines.add(ChatColor.GRAY + "------------------------");
        return lines;
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
        if (competitions.size() > 1) {
            lines.add(ChatColor.YELLOW + ChatColor.BOLD.toString() + "[ 點擊翻頁 ]  " +
                    ChatColor.GRAY + "第 1/" + (competitions.size() + 1) + " 頁");
        } else {
            lines.add(ChatColor.YELLOW + ChatColor.BOLD.toString() + "[ 點擊翻頁 ]");
        }

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

        if (isActive) {
            lines.add(ChatColor.GREEN + "狀態: " + ChatColor.BOLD + "進行中");
            lines.add(ChatColor.AQUA + "剩餘時間: " + ChatColor.WHITE + formatTime(activeCompetition.getTimeLeft()));
            lines.add(ChatColor.GRAY + "------------------------");

            List<CompetitionEntry> entries = activeCompetition.getLeaderboard() != null
                    ? activeCompetition.getLeaderboard().getEntries()
                    : new ArrayList<>();

            if (entries == null || entries.isEmpty()) {
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
        lines.add(ChatColor.YELLOW + ChatColor.BOLD.toString() + "[ 點擊翻頁 ]");

        return lines;
    }

    // ==================== 工具方法 ====================

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
        if (entry == null) return "0";
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
