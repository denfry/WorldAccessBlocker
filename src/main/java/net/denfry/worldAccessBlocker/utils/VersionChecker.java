package net.denfry.worldAccessBlocker.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class VersionChecker {

    private static final String API_URL =
            "https://api.modrinth.com/v2/project/worldaccessblocker/version?limit=1";

    private final JavaPlugin plugin;
    private final String currentVersion;
    private volatile String latestVersion = null; // null=pending, ""=up-to-date, "x.y.z"=new version

    public VersionChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    public void checkAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String response = fetchResponse();
                String latest = parseVersionFromResponse(response);
                if (latest != null && isNewer(latest, currentVersion)) {
                    latestVersion = latest;
                } else {
                    latestVersion = "";
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Version check failed: " + e.getMessage());
                latestVersion = "";
            }
        });
    }

    String fetchResponse() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("User-Agent",
                "WorldAccessBlocker/" + currentVersion + " (github.com/denfry)");
        try {
            if (conn.getResponseCode() != 200) {
                java.io.InputStream err = conn.getErrorStream();
                if (err != null) { err.close(); }
                return "[]";
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    static String parseVersionFromResponse(String json) {
        if (json == null || json.isBlank()) return null;
        String trimmed = json.trim();
        if (trimmed.equals("[]")) return null;
        int idx = trimmed.indexOf("\"version_number\"");
        if (idx < 0) return null;
        int colon = trimmed.indexOf(":", idx);
        if (colon < 0) return null;
        int start = trimmed.indexOf("\"", colon);
        if (start < 0) return null;
        start++;
        int end = trimmed.indexOf("\"", start);
        if (end < 0) return null;
        return trimmed.substring(start, end);
    }

    static boolean isNewer(String candidate, String current) {
        int[] c = parseSemver(candidate);
        int[] cur = parseSemver(current);
        if (c == null || cur == null) return false;
        for (int i = 0; i < 3; i++) {
            if (c[i] > cur[i]) return true;
            if (c[i] < cur[i]) return false;
        }
        return false;
    }

    private static int[] parseSemver(String v) {
        if (v == null) return null;
        String[] parts = v.split("\\.", 3);
        if (parts.length < 2) return null;
        try {
            int[] result = new int[3];
            result[0] = Integer.parseInt(parts[0].split("[-+]", 2)[0]);
            result[1] = Integer.parseInt(parts[1].split("[-+]", 2)[0]);
            result[2] = parts.length > 2 ? Integer.parseInt(parts[2].split("[-+]", 2)[0]) : 0;
            return result;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
