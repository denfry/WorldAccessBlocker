package net.denfry.worldAccessBlocker.utils;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import org.bukkit.Bukkit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class DiscordNotifier {
    private final WorldAccessBlocker plugin;
    private final HttpClient client = HttpClient.newHttpClient();

    public DiscordNotifier(WorldAccessBlocker plugin) {
        this.plugin = plugin;
    }

    public void send(String content) {
        if (!plugin.getConfigManager().isDiscordEnabled()) return;
        String url = plugin.getConfigManager().getDiscordWebhook();
        if (url == null || url.isBlank()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String payload = "{\"content\":\"" + content.replace("\"", "\\\"") + "\"}";
                HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                        .build();
                client.send(req, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                plugin.getLogger().fine("Discord webhook failed: " + e.getMessage());
            }
        });
    }
}

