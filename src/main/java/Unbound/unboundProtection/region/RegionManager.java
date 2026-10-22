package Unbound.unboundProtection.region;

import Unbound.unboundProtection.UnboundProtection;
import Unbound.unboundProtection.model.ProtectedRegion;
import Unbound.unboundProtection.model.ProtectionFlag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class RegionManager {
    private final UnboundProtection plugin;
    private final Map<String, ProtectedRegion> regions = new LinkedHashMap<>();
    private final File file;

    public RegionManager(UnboundProtection plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "regions.yml");
    }

    public void load() {
        regions.clear();
        if (!file.exists()) {
            save();
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("regions");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            ConfigurationSection data = section.getConfigurationSection(id);
            if (data == null) continue;
            String world = data.getString("world", "world");
            UUID ownerId = parseUuid(data.getString("owner.uuid"));
            String ownerName = data.getString("owner.name", "desconocido");
            ProtectedRegion region = new ProtectedRegion(
                    id, world,
                    data.getInt("min.x"), data.getInt("min.y"), data.getInt("min.z"),
                    data.getInt("max.x"), data.getInt("max.y"), data.getInt("max.z"),
                    ownerId, ownerName
            );
            for (String raw : data.getStringList("members")) {
                UUID uuid = parseUuid(raw);
                if (uuid != null) region.members().add(uuid);
            }
            ConfigurationSection flags = data.getConfigurationSection("flags");
            if (flags != null) {
                for (ProtectionFlag flag : ProtectionFlag.values()) {
                    if (flags.contains(flag.key())) region.setFlag(flag, flags.getBoolean(flag.key()));
                }
            }
            regions.put(region.id(), region);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (ProtectedRegion region : regions.values()) {
            String root = "regions." + region.id() + ".";
            yaml.set(root + "world", region.worldName());
            yaml.set(root + "owner.uuid", region.ownerId() == null ? null : region.ownerId().toString());
            yaml.set(root + "owner.name", region.ownerName());
            yaml.set(root + "min.x", region.minX());
            yaml.set(root + "min.y", region.minY());
            yaml.set(root + "min.z", region.minZ());
            yaml.set(root + "max.x", region.maxX());
            yaml.set(root + "max.y", region.maxY());
            yaml.set(root + "max.z", region.maxZ());
            yaml.set(root + "members", region.members().stream().map(UUID::toString).sorted().toList());
            for (ProtectionFlag flag : ProtectionFlag.values()) yaml.set(root + "flags." + flag.key(), region.flag(flag));
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("No se pudo crear la carpeta del plugin.");
                return;
            }
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("No se pudo guardar regions.yml: " + exception.getMessage());
        }
    }
    // cosito para crear
    public boolean create(ProtectedRegion region) {
        String key = normalize(region.id());
        if (regions.containsKey(key)) return false;
        regions.put(key, region);
        save();
        return true;
    }

    public ProtectedRegion get(String id) { return id == null ? null : regions.get(normalize(id)); }
    public boolean delete(String id) {
        if (id == null || regions.remove(normalize(id)) == null) return false;
        save();
        return true;
    }
    public Collection<ProtectedRegion> all() { return List.copyOf(regions.values()); }

    public List<ProtectedRegion> inWorld(String worldName) {
        return regions.values().stream().filter(region -> region.worldName().equalsIgnoreCase(worldName))
                .sorted(Comparator.comparing(ProtectedRegion::id)).toList();
    }

    public List<ProtectedRegion> at(Location location) {
        return regions.values().stream().filter(region -> region.contains(location))
                .sorted(Comparator.comparing(ProtectedRegion::id)).toList();
    }

    public boolean isBlocked(Location location, ProtectionFlag flag, org.bukkit.entity.Player player,
                             boolean bypass, boolean respectMembership) {
        if (bypass) return false;
        for (ProtectedRegion region : at(location)) {
            if (!region.flag(flag)) continue;
            if (respectMembership && flag.membersCanBuild() && region.isOwnerOrMember(player.getUniqueId())) continue;
            return true;
        }
        return false;
    }

    public boolean isBlockedForEnvironment(Location location, ProtectionFlag flag, boolean bypass) {
        if (bypass) return false;
        for (ProtectedRegion region : at(location)) if (region.flag(flag)) return true;
        return false;
    }

    public static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) return null;
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    public static String normalize(String id) { return id.toLowerCase(Locale.ROOT); }

    public static org.bukkit.World findWorld(String input) {
        if (input == null) return null;
        org.bukkit.World exact = Bukkit.getWorld(input);
        if (exact != null) return exact;
        return Bukkit.getWorlds().stream().filter(world -> world.getName().equalsIgnoreCase(input)).findFirst().orElse(null);
    }

    public static String ownerDisplay(ProtectedRegion region) {
        if (region.ownerId() == null) return region.ownerName();
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(region.ownerId());
        return offlinePlayer.getName() == null ? region.ownerName() : offlinePlayer.getName();
    }
}
