package com.dafealru.smartlogin.geoip;

import com.dafealru.smartlogin.SmartLogin;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class GeoIpManager {

    private final SmartLogin plugin;
    private final Map<String, GeoLocation> geoCache = new ConcurrentHashMap<>();
    private final Map<UUID, LastLoginLocation> lastKnownLocations = new ConcurrentHashMap<>();

    public record GeoLocation(String country, String countryCode, String city, String isp, boolean isVpnOrProxy) {}
    public record LastLoginLocation(String countryCode, String ip, long timestamp) {}

    public GeoIpManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<GeoLocation> lookup(String ip) {
        if (ip == null || ip.equals("127.0.0.1") || ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.16.")) {
            return CompletableFuture.completedFuture(new GeoLocation("Local / Private", "LOC", "Internal", "Localhost", false));
        }

        if (geoCache.containsKey(ip)) {
            return CompletableFuture.completedFuture(geoCache.get(ip));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Provider 1: ipwho.is (fast, includes security / proxy / hosting flags)
                URL url = new URL("http://ipwho.is/" + ip);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "SmartLogin-GeoEngine/2.0");
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);

                if (conn.getResponseCode() == 200) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        String json = sb.toString();

                        if (json.contains("\"success\":true") || json.contains("\"country\":")) {
                            String country = extractJsonField(json, "country");
                            String code = extractJsonField(json, "country_code");
                            String city = extractJsonField(json, "city");
                            String isp = extractJsonField(json, "isp");
                            boolean isVpn = json.contains("\"is_vpn\":true") || json.contains("\"is_proxy\":true") || json.contains("\"is_datacenter\":true");

                            GeoLocation loc = new GeoLocation(
                                    country.isEmpty() ? "Unknown" : country,
                                    code.isEmpty() ? "XX" : code,
                                    city.isEmpty() ? "Unknown" : city,
                                    isp.isEmpty() ? "Standard ISP" : isp,
                                    isVpn
                            );
                            geoCache.put(ip, loc);
                            return loc;
                        }
                    }
                }
            } catch (Exception ignored) {}

            // Fallback Provider 2: ipapi.co
            try {
                URL url = new URL("https://ipapi.co/" + ip + "/json/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "SmartLogin-GeoEngine/2.0");
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);

                if (conn.getResponseCode() == 200) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        String json = sb.toString();

                        String country = extractJsonField(json, "country_name");
                        String code = extractJsonField(json, "country_code");
                        String city = extractJsonField(json, "city");
                        String org = extractJsonField(json, "org");
                        boolean isHosting = org.toLowerCase().matches(".*(amazon|ovh|digitalocean|linode|hetzner|vultr|m247|choopa|leaseweb).*");

                        GeoLocation loc = new GeoLocation(
                                country.isEmpty() ? "Unknown Country" : country,
                                code.isEmpty() ? "XX" : code,
                                city.isEmpty() ? "Unknown" : city,
                                org.isEmpty() ? "ISP" : org,
                                isHosting
                        );
                        geoCache.put(ip, loc);
                        return loc;
                    }
                }
            } catch (Exception ignored) {}

            GeoLocation fallback = new GeoLocation("Unknown Country", "XX", "Unknown", "Unknown ISP", false);
            geoCache.put(ip, fallback);
            return fallback;
        });
    }

    public CompletableFuture<String> lookupCountry(String ip) {
        return lookup(ip).thenApply(GeoLocation::country);
    }

    public boolean checkImpossibleTravel(UUID uuid, String ip, String currentCountryCode) {
        if (currentCountryCode == null || currentCountryCode.equals("LOC") || currentCountryCode.equals("XX")) return false;

        LastLoginLocation last = lastKnownLocations.get(uuid);
        long now = System.currentTimeMillis();
        lastKnownLocations.put(uuid, new LastLoginLocation(currentCountryCode, ip, now));

        if (last != null) {
            long diffHours = (now - last.timestamp()) / (1000 * 60 * 60);
            // If country changed and occurred in less than 2 hours across different countries
            if (!last.countryCode().equalsIgnoreCase(currentCountryCode) && diffHours < 2) {
                return true; // Impossible Travel detected!
            }
        }
        return false;
    }

    private String extractJsonField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start == -1) return "";
        start += key.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }
}
