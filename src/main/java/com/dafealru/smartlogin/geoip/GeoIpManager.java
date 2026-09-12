package com.dafealru.smartlogin.geoip;

import com.dafealru.smartlogin.SmartLogin;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class GeoIpManager {

    private final SmartLogin plugin;
    private final Map<String, String> countryCache = new ConcurrentHashMap<>();

    public GeoIpManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<String> lookupCountry(String ip) {
        if (ip == null || ip.equals("127.0.0.1") || ip.startsWith("192.168.") || ip.startsWith("10.")) {
            return CompletableFuture.completedFuture("Localhost / Private");
        }

        if (countryCache.containsKey(ip)) {
            return CompletableFuture.completedFuture(countryCache.get(ip));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL("https://ipapi.co/" + ip + "/country_name/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "SmartLogin-GeoIP");
                conn.setConnectTimeout(2500);
                conn.setReadTimeout(2500);

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String country = reader.readLine();
                    if (country != null && !country.isEmpty()) {
                        countryCache.put(ip, country);
                        return country;
                    }
                }
            } catch (Exception ignored) {}
            return "Unknown Country";
        });
    }
}
