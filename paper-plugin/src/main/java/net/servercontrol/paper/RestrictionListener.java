package net.servercontrol.paper;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public final class RestrictionListener implements Listener {
    private final ServerControlPlugin plugin;
    private final ControlState state;

    public RestrictionListener(ServerControlPlugin plugin, ControlState state) {
        this.plugin = plugin;
        this.state = state;

        Bukkit.getScheduler().runTaskTimer(plugin, this::tickArmorDamage, 20L, 20L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (state.isPublicChatEnabled()) {
            return;
        }

        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () ->
                event.getPlayer().sendMessage(plugin.configuredComponent("messages.chat-blocked")));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        if (state.isDeathSoundEnabled()) {
            plugin.playDeathSoundNow();
        }

        if (!state.isDeathMessagesEnabled()) {
            event.deathMessage(null);
            event.setShowDeathMessages(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        if (!state.isJoinMessagesEnabled()) {
            event.joinMessage(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        if (!state.isQuitMessagesEnabled()) {
            event.quitMessage(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        if (!state.isAdvancementMessagesEnabled()) {
            event.message(null);
        }
    }

    private void tickArmorDamage() {
        if (!state.isArmorDamageEnabled()) {
            return;
        }

        double damage = state.armorDamageHealthPointsPerSecond();
        if (damage <= 0.0D) {
            return;
        }

        DamageSource damageSource = DamageSource.builder(DamageType.MAGIC).build();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isDead() || player.getGameMode() == GameMode.SPECTATOR || !isWearingDamageItem(player)) {
                continue;
            }

            player.damage(damage, damageSource);
        }
    }

    private boolean isWearingDamageItem(Player player) {
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && causesArmorDamage(item.getType())) {
                return true;
            }
        }
        return false;
    }

    private boolean causesArmorDamage(Material material) {
        if (material == Material.AIR) {
            return false;
        }
        if (material == Material.CARVED_PUMPKIN) {
            return state.isArmorDamageCarvedPumpkinsEnabled();
        }
        if (material == Material.ELYTRA) {
            return state.isArmorDamageElytrasEnabled();
        }
        if (isMobHead(material)) {
            return state.isArmorDamageMobHeadsEnabled();
        }
        return true;
    }

    private static boolean isMobHead(Material material) {
        return switch (material) {
            case PLAYER_HEAD,
                 CREEPER_HEAD,
                 DRAGON_HEAD,
                 PIGLIN_HEAD,
                 SKELETON_SKULL,
                 WITHER_SKELETON_SKULL,
                 ZOMBIE_HEAD -> true;
            default -> false;
        };
    }
}
