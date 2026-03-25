package me.ninepin.evenMoreFishLeadboard.commands;

import com.oheers.fish.EvenMoreFish;
import com.oheers.fish.competition.Competition;
import com.oheers.fish.competition.configs.CompetitionFile;
import com.oheers.fish.utils.TimeCode;
import me.ninepin.evenMoreFishLeadboard.EvenMoreFishLeadboard;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardCommand implements CommandExecutor, TabCompleter {

    private final EvenMoreFishLeadboard plugin;

    public LeaderboardCommand(EvenMoreFishLeadboard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "此指令只能由玩家執行！");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "setpos" -> {
                if (!player.hasPermission("evenmorefish.leaderboard.setpos")) {
                    player.sendMessage(ChatColor.RED + "你沒有權限執行此指令！");
                    return true;
                }
                Location loc = player.getLocation();
                plugin.setHologramLocation(loc);
                plugin.getHologramManager().removeAllHolograms();
                plugin.getHologramManager().showToAll();
                player.sendMessage(ChatColor.GREEN + "全息圖位置已設定！");
                player.sendMessage(ChatColor.AQUA + "位置: " + ChatColor.WHITE +
                        loc.getWorld().getName() +
                        " " + String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ()));
            }
            case "status" -> {
                if (!player.hasPermission("evenmorefish.leaderboard.admin")) {
                    player.sendMessage(ChatColor.RED + "你沒有權限執行此指令！");
                    return true;
                }
                showStatus(player);
            }
            case "listids" -> {
                if (!player.hasPermission("evenmorefish.leaderboard.admin")) {
                    player.sendMessage(ChatColor.RED + "你沒有權限執行此指令！");
                    return true;
                }
                listCompetitionIds(player);
            }
            case "reload" -> {
                if (!player.hasPermission("evenmorefish.leaderboard.admin")) {
                    player.sendMessage(ChatColor.RED + "你沒有權限執行此指令！");
                    return true;
                }
                plugin.reloadConfig();
                plugin.getHologramManager().removeAllHolograms();
                player.sendMessage(ChatColor.GREEN + "排行榜插件已重新載入！");
            }
            case "help" -> sendHelp(player);
            default -> {
                player.sendMessage(ChatColor.RED + "未知的子指令！使用 /flb help 查看說明");
            }
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "========== " + ChatColor.YELLOW + "釣魚競賽排行榜說明" + ChatColor.GOLD + " ==========");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "/flb setpos" + ChatColor.WHITE + " - 設定全息圖顯示位置 (管理員)");
        player.sendMessage(ChatColor.YELLOW + "/flb status" + ChatColor.WHITE + " - 顯示目前競賽狀態 (管理員)");
        player.sendMessage(ChatColor.YELLOW + "/flb listids" + ChatColor.WHITE + " - 列出所有競賽 ID (管理員)");
        player.sendMessage(ChatColor.YELLOW + "/flb reload" + ChatColor.WHITE + " - 重新載入插件 (管理員)");
        player.sendMessage(ChatColor.YELLOW + "/flb help" + ChatColor.WHITE + " - 顯示此說明資訊");
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "提示: 點擊全息圖可切換顯示的比賽");
    }

    private void showStatus(Player player) {
        boolean isActive = Competition.isActive();

        player.sendMessage(ChatColor.GOLD + "=== 競賽狀態 ===");
        player.sendMessage(ChatColor.AQUA + "目前狀態: " +
                (isActive ? ChatColor.GREEN + "進行中" : ChatColor.RED + "無進行中競賽"));

        if (isActive) {
            Competition competition = Competition.getCurrentlyActive();
            if (competition != null) {
                player.sendMessage(ChatColor.AQUA + "競賽名稱: " + ChatColor.WHITE + competition.getCompetitionName());
                player.sendMessage(ChatColor.AQUA + "參賽人數: " + ChatColor.WHITE + competition.getLeaderboardSize());
                player.sendMessage(ChatColor.AQUA + "剩餘時間: " + ChatColor.WHITE +
                        formatTime(competition.getTimeLeft()));
            }
        }

        try {
            Map<TimeCode, CompetitionFile> competitions =
                    EvenMoreFish.getInstance().getCompetitionQueue().getCompetitions();
            if (competitions != null) {
                Set<String> uniqueIds = competitions.values().stream()
                        .map(CompetitionFile::getId)
                        .collect(Collectors.toSet());
                player.sendMessage(ChatColor.AQUA + "已設定競賽: " + ChatColor.WHITE + uniqueIds.size() + " 個");
            }
        } catch (Exception ignored) {}
    }

    private void listCompetitionIds(Player player) {
        try {
            Map<TimeCode, CompetitionFile> competitions =
                    EvenMoreFish.getInstance().getCompetitionQueue().getCompetitions();

            if (competitions == null || competitions.isEmpty()) {
                player.sendMessage(ChatColor.RED + "沒有找到任何競賽設定！");
                return;
            }

            Set<String> uniqueIds = competitions.values().stream()
                    .map(CompetitionFile::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            player.sendMessage(ChatColor.GOLD + "=== 競賽 ID 列表 ===");
            for (String id : uniqueIds) {
                player.sendMessage(ChatColor.YELLOW + " - " + ChatColor.WHITE + id);
            }

        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "取得競賽列表時發生錯誤: " + e.getMessage());
        }
    }

    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("setpos", "status", "listids", "reload", "help");
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();

            for (String subCmd : subCommands) {
                if (subCmd.startsWith(input)) {
                    completions.add(subCmd);
                }
            }
            return completions;
        }
        return new ArrayList<>();
    }
}
