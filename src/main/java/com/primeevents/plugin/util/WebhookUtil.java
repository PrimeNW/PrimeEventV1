package com.primeevents.plugin.util;

import com.primeevents.plugin.PrimeEvents;
import org.bukkit.ChatColor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class WebhookUtil {

    /** Basit gonderim: config'teki tum varsayilanlari kullanir (baslik/renk/thumbnail/ping). */
    public static void send(PrimeEvents plugin, String notifyKey, String plainMessage) {
        send(plugin, notifyKey, plainMessage, null, null, null);
    }

    /**
     * @param titleOverride null ise webhook.embed.title kullanilir, degilse bu baslik kullanilir.
     * @param colorOverride null ise webhook.embed.color kullanilir, degilse bu renk (#RRGGBB) kullanilir.
     */
    public static void send(PrimeEvents plugin, String notifyKey, String plainMessage, String titleOverride, String colorOverride) {
        send(plugin, notifyKey, plainMessage, titleOverride, colorOverride, null);
    }

    /**
     * @param rewardCount null degilse embed'e "Odul Sayisi" alani eklenir (odul dagitim duyurusu icin).
     */
    public static void send(PrimeEvents plugin, String notifyKey, String plainMessage, String titleOverride, String colorOverride, Integer rewardCount) {
        boolean debug = plugin.getConfig().getBoolean("debug", false);

        if (!plugin.getConfig().getBoolean("webhook.enabled", false)) {
            if (debug) plugin.getLogger().info("[debug][webhook] gonderilmedi: webhook.enabled=false");
            return;
        }
        if (!plugin.getConfig().getBoolean("webhook.notify." + notifyKey, false)) {
            if (debug) plugin.getLogger().info("[debug][webhook] gonderilmedi: webhook.notify." + notifyKey + "=false (veya config'te yok)");
            return;
        }

        String url = plugin.getConfig().getString("webhook.url", "");
        if (url == null || url.isEmpty() || url.contains("XXXXXXXX")) {
            plugin.getLogger().warning("[webhook] gonderilmedi: webhook.url ayarlanmamis veya hala varsayilan placeholder.");
            return;
        }

        String username = plugin.getConfig().getString("webhook.username", "PrimeEvents");
        String avatar = plugin.getConfig().getString("webhook.avatar-url", "");
        String cleanMessage = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', plainMessage));

        String embedTitle = titleOverride != null ? titleOverride
                : plugin.getConfig().getString("webhook.embed.title", "PrimeEvents");
        String embedColor = colorOverride != null ? colorOverride
                : plugin.getConfig().getString("webhook.embed.color", "#FFD700");
        String embedFooter = plugin.getConfig().getString("webhook.embed.footer", "PrimeEvents");
        String thumbnail = plugin.getConfig().getString("webhook.embed.thumbnail", "");
        String image = plugin.getConfig().getString("webhook.embed.image", "");
        boolean pingEveryone = plugin.getConfig().getBoolean("webhook.everyone-ping", true);

        // Bukkit scheduler asenkron gorev - ana thread'i bloklamamak icin
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                doSend(plugin, url, username, avatar, embedTitle, cleanMessage, embedColor, embedFooter,
                        thumbnail, image, rewardCount, pingEveryone, debug));
    }

    /** /event webhooktest icin: senkron calisir, sonucu direkt donuyor. */
    public static String sendTestSync(PrimeEvents plugin) {
        String url = plugin.getConfig().getString("webhook.url", "");
        if (url == null || url.isEmpty() || url.contains("XXXXXXXX")) {
            return "webhook.url ayarlanmamis veya hala placeholder icin.";
        }
        String username = plugin.getConfig().getString("webhook.username", "PrimeEvents");
        String avatar = plugin.getConfig().getString("webhook.avatar-url", "");
        String embedTitle = plugin.getConfig().getString("webhook.embed.title", "PrimeEvents");
        String embedColor = plugin.getConfig().getString("webhook.embed.color", "#FFD700");
        String embedFooter = plugin.getConfig().getString("webhook.embed.footer", "PrimeEvents");
        String thumbnail = plugin.getConfig().getString("webhook.embed.thumbnail", "");
        String image = plugin.getConfig().getString("webhook.embed.image", "");
        boolean pingEveryone = plugin.getConfig().getBoolean("webhook.everyone-ping", true);
        return doSend(plugin, url, username, avatar, embedTitle,
                "PrimeEvents test mesaji - webhook calisiyor!", embedColor, embedFooter,
                thumbnail, image, null, pingEveryone, true);
    }

    private static String doSend(PrimeEvents plugin, String url, String username, String avatar,
                                  String embedTitle, String description, String embedColor, String embedFooter,
                                  String thumbnail, String image, Integer rewardCount, boolean pingEveryone,
                                  boolean debug) {
        HttpURLConnection conn = null;
        try {
            String json = buildEmbedJson(username, avatar, embedTitle, description, embedColor, embedFooter,
                    thumbnail, image, rewardCount, pingEveryone);
            if (debug) plugin.getLogger().info("[debug][webhook] POST " + url + " body=" + json);

            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("User-Agent", "PrimeEvents-Plugin/1.0");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setInstanceFollowRedirects(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                if (debug) plugin.getLogger().info("[debug][webhook] basarili, HTTP " + code);
                return "OK (HTTP " + code + ")";
            }

            String body = readStream(code >= 400 ? conn.getErrorStream() : conn.getInputStream());
            String result = "HTTP " + code + " - " + body;
            plugin.getLogger().warning("[webhook] gonderim basarisiz: " + result);
            return result;

        } catch (Exception ex) {
            plugin.getLogger().warning("[webhook] gonderim hatasi: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            if (debug) ex.printStackTrace();
            return "Hata: " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String readStream(InputStream is) {
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Discord embed payload'i olusturur: baslik + aciklama + renk + footer + timestamp,
     * opsiyonel thumbnail (sag ust kucuk resim), image (alt buyuk banner), odul sayisi alani
     * ve @everyone ping'i icerir.
     */
    private static String buildEmbedJson(String username, String avatar, String title, String description,
                                          String colorHex, String footer, String thumbnail, String image,
                                          Integer rewardCount, boolean pingEveryone) {
        int colorDecimal = parseColor(colorHex);
        String timestamp = Instant.now().toString();

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (pingEveryone) {
            sb.append("\"content\":\"@everyone\",");
        }
        sb.append("\"username\":\"").append(escape(username)).append("\",");
        if (avatar != null && !avatar.isEmpty()) {
            sb.append("\"avatar_url\":\"").append(escape(avatar)).append("\",");
        }
        sb.append("\"embeds\":[{");
        sb.append("\"title\":\"").append(escape(title)).append("\",");
        sb.append("\"description\":\"").append(escape(description)).append("\",");
        sb.append("\"color\":").append(colorDecimal).append(",");
        sb.append("\"timestamp\":\"").append(timestamp).append("\",");
        if (thumbnail != null && !thumbnail.isEmpty()) {
            sb.append("\"thumbnail\":{\"url\":\"").append(escape(thumbnail)).append("\"},");
        }
        if (image != null && !image.isEmpty()) {
            sb.append("\"image\":{\"url\":\"").append(escape(image)).append("\"},");
        }
        if (rewardCount != null) {
            sb.append("\"fields\":[{\"name\":\"🎁 Odul Sayisi\",\"value\":\"")
                    .append(rewardCount).append(" item\",\"inline\":true}],");
        }
        sb.append("\"footer\":{\"text\":\"").append(escape(footer)).append("\"}");
        sb.append("}]");
        if (pingEveryone) {
            sb.append(",\"allowed_mentions\":{\"parse\":[\"everyone\"]}");
        }
        sb.append("}");
        return sb.toString();
    }

    /** "#FFD700" gibi hex renk kodunu Discord'un bekledigi decimal degere cevirir. */
    private static int parseColor(String hex) {
        try {
            String clean = hex.replace("#", "").trim();
            return Integer.parseInt(clean, 16);
        } catch (Exception e) {
            return 0xFFD700; // fallback altin sarisi
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
