package me.ninepin.evenMoreFishLeadboard;

import me.ninepin.evenMoreFishLeadboard.commands.LeaderboardCommand;
import me.ninepin.evenMoreFishLeadboard.hologram.HologramManager;
import me.ninepin.evenMoreFishLeadboard.hologram.HologramUpdateTask;
import me.ninepin.evenMoreFishLeadboard.listener.CompetitionListener;
import me.ninepin.evenMoreFishLeadboard.listener.HologramClickListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class EvenMoreFishLeadboard extends JavaPlugin {

    private static EvenMoreFishLeadboard instance;
    private HologramManager hologramManager;
    private HologramUpdateTask updateTask;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        if (!checkDependencies()) {
            getLogger().severe("缺少必要依賴插件，插件將被停用！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Location holoLoc = getHologramLocation();
        if (holoLoc == null) {
            getLogger().warning("全息圖位置未設定或世界不存在！請使用 /flb setpos 設定位置");
        } else {
            getLogger().info("全息圖位置: " + holoLoc.getWorld().getName() +
                    " " + String.format("%.1f, %.1f, %.1f", holoLoc.getX(), holoLoc.getY(), holoLoc.getZ()));
        }

        hologramManager = new HologramManager(this);

        // 啟動自動更新任務（每秒更新一次）
        updateTask = new HologramUpdateTask(this, hologramManager);
        updateTask.runTaskTimer(this, 2L, 2L);

        // 註冊事件監聽器
        getServer().getPluginManager().registerEvents(new CompetitionListener(this), this);
        getServer().getPluginManager().registerEvents(new HologramClickListener(this, hologramManager), this);

        // 註冊命令
        LeaderboardCommand command = new LeaderboardCommand(this);
        getCommand("fishleaderboard").setExecutor(command);
        getCommand("fishleaderboard").setTabCompleter(command);

        // 預設顯示給所有線上玩家
        for (Player player : Bukkit.getOnlinePlayers()) {
            hologramManager.showToPlayer(player);
        }

        getLogger().info("EvenMoreFish Leaderboard 插件已啟用！");
    }

    @Override
    public void onDisable() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        if (hologramManager != null) {
            hologramManager.removeAllHolograms();
        }
        getLogger().info("EvenMoreFish Leaderboard 插件已停用！");
    }

    public Location getHologramLocation() {
        String worldName = getConfig().getString("hologram-location.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        double x = getConfig().getDouble("hologram-location.x", 0.0);
        double y = getConfig().getDouble("hologram-location.y", 100.0);
        double z = getConfig().getDouble("hologram-location.z", 0.0);

        return new Location(world, x, y, z);
    }

    public void setHologramLocation(Location location) {
        getConfig().set("hologram-location.world", location.getWorld().getName());
        getConfig().set("hologram-location.x", location.getX());
        getConfig().set("hologram-location.y", location.getY());
        getConfig().set("hologram-location.z", location.getZ());
        saveConfig();
    }

    private boolean checkDependencies() {
        boolean hasDependencies = true;

        if (getServer().getPluginManager().getPlugin("EvenMoreFish") == null) {
            getLogger().warning("未找到 EvenMoreFish 插件！");
            hasDependencies = false;
        }

        if (getServer().getPluginManager().getPlugin("DecentHolograms") == null) {
            getLogger().warning("未找到 DecentHolograms 插件！");
            hasDependencies = false;
        }

        return hasDependencies;
    }

    public static EvenMoreFishLeadboard getInstance() {
        return instance;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }
}
