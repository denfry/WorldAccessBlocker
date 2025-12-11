package net.denfry.worldAccessBlocker.utils;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.time.Instant;
import java.util.UUID;

public class SyncManager implements PluginMessageListener {
    private final WorldAccessBlocker plugin;

    public SyncManager(WorldAccessBlocker plugin) {
        this.plugin = plugin;
    }

    public void register() {
        if (!plugin.getConfigManager().isSyncEnabled()) return;
        String channel = plugin.getConfigManager().getSyncChannel();
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, channel);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, channel, this);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(plugin.getConfigManager().getSyncChannel())) return;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String action = in.readUTF();
            UUID uuid = UUID.fromString(in.readUTF());
            String feature = in.readUTF();
            if ("grant".equalsIgnoreCase(action)) {
                long until = in.readLong();
                String reason = in.readUTF();
                plugin.getBypassManager().grantBypass(uuid, feature, Instant.ofEpochSecond(until), reason, false);
            } else if ("remove".equalsIgnoreCase(action)) {
                plugin.getBypassManager().removeBypass(uuid, feature);
            }
        } catch (IOException ignored) {
        }
    }

    public void broadcastGrant(UUID uuid, String feature, Instant until, String reason) {
        if (!plugin.getConfigManager().isSyncEnabled()) return;
        send("grant", uuid, feature, until, reason);
    }

    public void broadcastRemove(UUID uuid, String feature) {
        if (!plugin.getConfigManager().isSyncEnabled()) return;
        send("remove", uuid, feature, Instant.EPOCH, "");
    }

    private void send(String action, UUID uuid, String feature, Instant until, String reason) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             DataOutputStream data = new DataOutputStream(out)) {
            data.writeUTF(action);
            data.writeUTF(uuid.toString());
            data.writeUTF(feature);
            data.writeLong(until.getEpochSecond());
            data.writeUTF(reason == null ? "" : reason);
            byte[] payload = out.toByteArray();
            String ch = plugin.getConfigManager().getSyncChannel();
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendPluginMessage(plugin, ch, payload);
                break; // send via any player
            }
        } catch (IOException ignored) {
        }
    }
}

