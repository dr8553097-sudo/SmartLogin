package com.dafealru.smartlogin.spawn;

import com.dafealru.smartlogin.SmartLogin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpawnManager {

    private final SmartLogin plugin;
    private final Map<UUID, Location> previousLocations = new HashMap<>();

    public SpawnManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public Location getAuthSpawn() {
        FileConfiguration c = plugin.getModularConfig().getConfig();
        if (!c.getBoolean("auth-spawn.enabled", true)) return null;

        String worldName = c.getString("auth-spawn.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }
        if (world == null) return null;

        double x = c.getDouble("auth-spawn.x", 0.5);
        double y = c.getDouble("auth-spawn.y", 64.0);
        double z = c.getDouble("auth-spawn.z", 0.5);
        float yaw = (float) c.getDouble("auth-spawn.yaw", 0.0);
        float pitch = (float) c.getDouble("auth-spawn.pitch", 0.0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    public void setAuthSpawn(Location loc) {
        FileConfiguration c = plugin.getModularConfig().getConfig();
        c.set("auth-spawn.enabled", true);
        c.set("auth-spawn.world", loc.getWorld().getName());
        c.set("auth-spawn.x", loc.getX());
        c.set("auth-spawn.y", loc.getY());
        c.set("auth-spawn.z", loc.getZ());
        c.set("auth-spawn.yaw", loc.getYaw());
        c.set("auth-spawn.pitch", loc.getPitch());
        plugin.getModularConfig().saveConfig();
    }

    public void handleJoinSpawn(Player player) {
        Location spawn = getAuthSpawn();
        if (spawn != null) {
            previousLocations.put(player.getUniqueId(), player.getLocation());
            player.teleportAsync(spawn);
        }
    }

    public void handleLoginRestore(Player player) {
        FileConfiguration c = plugin.getModularConfig().getConfig();
        if (c.getBoolean("auth-spawn.teleport-back-on-login", true)) {
            Location prev = previousLocations.remove(player.getUniqueId());
            if (prev != null) {
                player.teleportAsync(prev);
            }
        }
    }
}
