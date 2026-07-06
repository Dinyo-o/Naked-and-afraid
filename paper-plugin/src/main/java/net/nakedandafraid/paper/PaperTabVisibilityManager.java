package net.nakedandafraid.paper;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

public final class PaperTabVisibilityManager implements Listener {
    private final NakedAndAfraidPlugin plugin;
    private final ControlState state;
    private BukkitTask refreshTask;
    private boolean managingTabList;

    public PaperTabVisibilityManager(NakedAndAfraidPlugin plugin, ControlState state) {
        this.plugin = plugin;
        this.state = state;
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::applyAll, 20L, 40L);
    }

    public void unregister() {
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        if (managingTabList) {
            restoreAll();
        }
        managingTabList = false;
    }

    public void applyAll() {
        if (state.isTabListEnabled()) {
            if (managingTabList) {
                restoreAll();
                managingTabList = false;
            }
            return;
        }

        managingTabList = true;
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            boolean viewerCanSeeTab = canViewerSeeTab(viewer);
            for (Player listedPlayer : Bukkit.getOnlinePlayers()) {
                setListed(viewer, listedPlayer, viewerCanSeeTab);
            }
        }
    }

    private boolean canViewerSeeTab(Player viewer) {
        return state.canSpectatorsSeeTabList() && viewer.getGameMode() == GameMode.SPECTATOR;
    }

    private void restoreAll() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (Player listedPlayer : Bukkit.getOnlinePlayers()) {
                setListed(viewer, listedPlayer, true);
            }
        }
    }

    private void setListed(Player viewer, Player listedPlayer, boolean listed) {
        try {
            if (listed) {
                viewer.listPlayer(listedPlayer);
            } else {
                viewer.unlistPlayer(listedPlayer);
            }
        } catch (IllegalArgumentException ignored) {
            // Some setups may reject changing the viewer's own tab entry.
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        applyAll();
        Bukkit.getScheduler().runTask(plugin, this::applyAll);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTask(plugin, this::applyAll);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Bukkit.getScheduler().runTask(plugin, this::applyAll);
    }
}
