package net.servercontrol.paper;

import net.kyori.adventure.text.Component;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public final class HandshakeManager implements Listener, PluginMessageListener {
    private static final long HELLO_RETRY_TICKS = 10L;

    private final ServerControlPlugin plugin;
    private final ControlState state;
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentMap<UUID, PendingHandshake> pendingHandshakes = new ConcurrentHashMap<>();
    private final Set<UUID> verifiedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> unverifiedJoinKicks = ConcurrentHashMap.newKeySet();

    public HandshakeManager(ServerControlPlugin plugin, ControlState state) {
        this.plugin = plugin;
        this.state = state;
    }

    public void register() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, ServerControlProtocol.CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, ServerControlProtocol.CHANNEL, this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void unregister() {
        pendingHandshakes.values().forEach(PendingHandshake::cancel);
        pendingHandshakes.clear();
        verifiedPlayers.clear();
        unverifiedJoinKicks.clear();
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, ServerControlProtocol.CHANNEL, this);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, ServerControlProtocol.CHANNEL);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!plugin.requireClientMod()) {
            Bukkit.getScheduler().runTask(plugin, () -> sendState(player));
            return;
        }

        Component joinMessage = event.joinMessage();
        beginPlayHandshake(player, joinMessage);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoinFinalize(PlayerJoinEvent event) {
        PendingHandshake pending = pendingHandshakes.get(event.getPlayer().getUniqueId());
        if (pending == null) {
            return;
        }

        pending.joinMessage = event.joinMessage();
        event.joinMessage(null);
    }

    @EventHandler
    public void onRegisterChannel(PlayerRegisterChannelEvent event) {
        if (!ServerControlProtocol.CHANNEL.equals(event.getChannel())) {
            return;
        }

        PendingHandshake pending = pendingHandshakes.get(event.getPlayer().getUniqueId());
        if (pending != null) {
            sendHello(event.getPlayer(), pending);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (pendingHandshakes.containsKey(event.getPlayer().getUniqueId()) && event.hasChangedPosition()) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (pendingHandshakes.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (pendingHandshakes.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (pendingHandshakes.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (pendingHandshakes.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (pendingHandshakes.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        if (pendingHandshakes.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && pendingHandshakes.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        PendingHandshake pending = pendingHandshakes.remove(playerId);
        if (pending != null) {
            pending.cancel();
        }

        if (unverifiedJoinKicks.remove(playerId)) {
            event.quitMessage(null);
        }
        verifiedPlayers.remove(playerId);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!ServerControlProtocol.CHANNEL.equals(channel)) {
            return;
        }

        ServerControlProtocol.decode(message).ifPresent(packet -> {
            if (packet.opcode() == ServerControlProtocol.OP_CLIENT_READY) {
                if (!plugin.serverControlVersion().equals(packet.releaseVersion())) {
                    rejectVersionMismatch(player, packet.releaseVersion());
                    return;
                }

                PendingHandshake pending = pendingHandshakes.get(player.getUniqueId());
                if (pending != null) {
                    sendHello(player, pending);
                }
                return;
            }

            if (packet.opcode() == ServerControlProtocol.OP_HELLO_ACK) {
                verifyAck(player, packet.nonce(), packet.releaseVersion());
            }
        });
    }

    public void sendState(Player player) {
        if (!player.isOnline()) {
            return;
        }

        player.sendPluginMessage(plugin, ServerControlProtocol.CHANNEL, ServerControlProtocol.state(state));
    }

    public void broadcastState() {
        byte[] payload = ServerControlProtocol.state(state);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.requireClientMod() || verifiedPlayers.contains(player.getUniqueId())) {
                player.sendPluginMessage(plugin, ServerControlProtocol.CHANNEL, payload);
            }
        }
    }

    public void forEachVerifiedPlayer(Consumer<Player> action) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.requireClientMod() || verifiedPlayers.contains(player.getUniqueId())) {
                action.accept(player);
            }
        }
    }

    private void beginPlayHandshake(Player player, Component joinMessage) {
        UUID playerId = player.getUniqueId();
        if (verifiedPlayers.contains(playerId) || pendingHandshakes.containsKey(playerId)) {
            return;
        }

        PendingHandshake pending = new PendingHandshake(random.nextLong(), joinMessage, state.isJoinMessagesEnabled());
        isolatePendingPlayer(player);
        BukkitTask retryTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player onlinePlayer = Bukkit.getPlayer(playerId);
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                PendingHandshake removed = pendingHandshakes.remove(playerId);
                if (removed != null) {
                    removed.cancel();
                }
                return;
            }

            PendingHandshake current = pendingHandshakes.get(playerId);
            if (current != null && onlinePlayer.getListeningPluginChannels().contains(ServerControlProtocol.CHANNEL)) {
                sendHello(onlinePlayer, current);
            }
        }, 1L, HELLO_RETRY_TICKS);
        pending.retryTask = retryTask;

        BukkitTask timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PendingHandshake removed = pendingHandshakes.remove(playerId);
            if (removed == null || verifiedPlayers.contains(playerId)) {
                return;
            }

            removed.cancel();
            Player onlinePlayer = Bukkit.getPlayer(playerId);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                unverifiedJoinKicks.add(playerId);
                plugin.getLogger().info("Rejected " + onlinePlayer.getName()
                        + ": no ServerControl play handshake. Listening channels: "
                        + onlinePlayer.getListeningPluginChannels());
                onlinePlayer.kick(kickMessage());
            }
        }, timeoutTicks());
        pending.timeoutTask = timeoutTask;

        pendingHandshakes.put(playerId, pending);
    }

    private void sendHello(Player player, PendingHandshake pending) {
        if (!player.isOnline()) {
            return;
        }

        player.sendPluginMessage(
                plugin,
                ServerControlProtocol.CHANNEL,
                ServerControlProtocol.hello(pending.nonce, state, plugin.serverControlVersion())
        );
    }

    private void verifyAck(Player player, long nonce, String clientVersion) {
        UUID playerId = player.getUniqueId();
        PendingHandshake pending = pendingHandshakes.get(playerId);
        if (pending == null || pending.nonce != nonce) {
            return;
        }

        if (!plugin.serverControlVersion().equals(clientVersion)) {
            rejectVersionMismatch(player, clientVersion);
            return;
        }

        pendingHandshakes.remove(playerId);
        pending.cancel();
        verifiedPlayers.add(playerId);
        restoreVerifiedPlayer(player);
        plugin.getLogger().info("Verified ServerControl client for " + player.getName() + " by play handshake.");
        if (pending.joinMessagesEnabledAtJoin && state.isJoinMessagesEnabled() && pending.joinMessage != null) {
            Bukkit.broadcast(pending.joinMessage);
        }
        sendState(player);
        plugin.customDeathSoundManager().syncTo(player);
    }

    private void rejectVersionMismatch(Player player, String clientVersion) {
        UUID playerId = player.getUniqueId();
        PendingHandshake pending = pendingHandshakes.remove(playerId);
        if (pending != null) {
            pending.cancel();
        }

        if (!player.isOnline()) {
            return;
        }

        String displayedClientVersion = clientVersion == null || clientVersion.isBlank() ? "unknown" : clientVersion;
        unverifiedJoinKicks.add(playerId);
        plugin.getLogger().info("Rejected " + player.getName()
                + ": ServerControl client version " + displayedClientVersion
                + " does not match server plugin version " + plugin.serverControlVersion() + ".");
        player.kick(Component.text("ServerControl client/server version mismatch. Install ServerControl "
                + plugin.serverControlVersion() + "."));
    }

    private void isolatePendingPlayer(Player pendingPlayer) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(pendingPlayer)) {
                continue;
            }

            pendingPlayer.unlistPlayer(other);
            other.unlistPlayer(pendingPlayer);
        }
    }

    private void restoreVerifiedPlayer(Player verifiedPlayer) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(verifiedPlayer) || pendingHandshakes.containsKey(other.getUniqueId())) {
                continue;
            }

            verifiedPlayer.listPlayer(other);
            other.listPlayer(verifiedPlayer);
        }
    }

    private long timeoutTicks() {
        return Math.max(20L, (plugin.handshakeTimeoutMillis() + 49L) / 50L);
    }

    private Component kickMessage() {
        return plugin.configuredComponent("handshake.kick-message");
    }

    private static final class PendingHandshake {
        private final long nonce;
        private final boolean joinMessagesEnabledAtJoin;
        private Component joinMessage;
        private BukkitTask retryTask;
        private BukkitTask timeoutTask;

        private PendingHandshake(long nonce, Component joinMessage, boolean joinMessagesEnabledAtJoin) {
            this.nonce = nonce;
            this.joinMessage = joinMessage;
            this.joinMessagesEnabledAtJoin = joinMessagesEnabledAtJoin;
        }

        private void cancel() {
            if (retryTask != null) {
                retryTask.cancel();
            }
            if (timeoutTask != null) {
                timeoutTask.cancel();
            }
        }
    }
}
