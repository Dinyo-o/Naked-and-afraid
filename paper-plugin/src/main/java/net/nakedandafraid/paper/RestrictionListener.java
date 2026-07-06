package net.nakedandafraid.paper;

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
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public final class RestrictionListener implements Listener {
    private static final Set<String> PUBLIC_MESSAGE_COMMANDS = Set.of(
            "me",
            "minecraft:me",
            "bukkit:me",
            "say",
            "minecraft:say",
            "bukkit:say",
            "broadcast",
            "bukkit:broadcast",
            "bc",
            "essentials:broadcast",
            "essentials:bc"
    );

    private final NakedAndAfraidPlugin plugin;
    private final ControlState state;

    public RestrictionListener(NakedAndAfraidPlugin plugin, ControlState state) {
        this.plugin = plugin;
        this.state = state;

        Bukkit.getScheduler().runTaskTimer(plugin, this::tickArmorDamage, 20L, 20L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (suppressPublicChat(event)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () ->
                event.getPlayer().sendMessage(plugin.configuredComponent("messages.chat-blocked")));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChatFinalize(AsyncChatEvent event) {
        suppressPublicChat(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPublicMessageCommand(PlayerCommandPreprocessEvent event) {
        if (state.isPublicChatEnabled() || canBypassPublicChat(event.getPlayer())) {
            return;
        }
        if (!PUBLIC_MESSAGE_COMMANDS.contains(commandRoot(event.getMessage()))) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(plugin.configuredComponent("messages.chat-blocked"));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        if (state.isDeathSoundEnabled()) {
            plugin.playDeathSoundNow();
        }

        suppressDeathMessage(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeathFinalize(PlayerDeathEvent event) {
        suppressDeathMessage(event);
    }

    private void suppressDeathMessage(PlayerDeathEvent event) {
        if (!state.isDeathMessagesEnabled()) {
            event.deathMessage(null);
            event.setShowDeathMessages(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        suppressJoinMessage(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoinFinalize(PlayerJoinEvent event) {
        suppressJoinMessage(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        suppressQuitMessage(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuitFinalize(PlayerQuitEvent event) {
        suppressQuitMessage(event);
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

    private boolean suppressPublicChat(AsyncChatEvent event) {
        if (state.isPublicChatEnabled()) {
            return true;
        }
        event.setCancelled(true);
        return false;
    }

    private static boolean canBypassPublicChat(Player player) {
        return player.isOp() || player.hasPermission(NakedAndAfraidPermissions.ALL);
    }

    private static String commandRoot(String message) {
        String trimmed = message.strip();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        int space = trimmed.indexOf(' ');
        String root = space >= 0 ? trimmed.substring(0, space) : trimmed;
        return root.toLowerCase(java.util.Locale.ROOT);
    }

    private void suppressJoinMessage(PlayerJoinEvent event) {
        if (!state.isJoinMessagesEnabled()) {
            event.joinMessage(null);
        }
    }

    private void suppressQuitMessage(PlayerQuitEvent event) {
        if (!state.isQuitMessagesEnabled()) {
            event.quitMessage(null);
        }
    }
}
