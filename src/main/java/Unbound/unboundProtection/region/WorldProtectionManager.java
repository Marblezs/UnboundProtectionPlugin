package Unbound.unboundProtection.region;

import Unbound.unboundProtection.UnboundProtection;
import Unbound.unboundProtection.model.ProtectionFlag;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class WorldProtectionManager {
    private final UnboundProtection plugin;
    private final File file;
    private final Map<String, Map<ProtectionFlag, Boolean>> worlds = new LinkedHashMap<>();

    public WorldProtectionManager(UnboundProtection plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "worlds.yml");
    }

    public void load() {
        worlds.clear();
        if (!file.exists()) {
            save();
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("worlds");
        if (section == null) return;
        for (String worldName : section.getKeys(false)) {
            ConfigurationSection flagsSection = section.getConfigurationSection(worldName + ".flags");
            Map<ProtectionFlag, Boolean> flags = new EnumMap<>(ProtectionFlag.class);
            if (flagsSection != null) {
                for (ProtectionFlag flag : ProtectionFlag.values()) {
                    if (flagsSection.contains(flag.key())) flags.put(flag, flagsSection.getBoolean(flag.key()));
                }
            }
            worlds.put(normalize(worldName), flags);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Map<ProtectionFlag, Boolean>> world : worlds.entrySet()) {
            for (Map.Entry<ProtectionFlag, Boolean> flag : world.getValue().entrySet()) {
                yaml.set("worlds." + world.getKey() + ".flags." + flag.getKey().key(), flag.getValue());
            }
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("no se pudo crear la carpeta del plugin.");
                return;
            }
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("no se pudo guardar worlds.yml: " + exception.getMessage());
        }
    }

    public boolean isEnabled(Location location, ProtectionFlag flag) {
        if (location.getWorld() == null) return false;
        Map<ProtectionFlag, Boolean> flags = worlds.get(normalize(location.getWorld().getName()));
        return flags != null && flags.getOrDefault(flag, false);
    }

    public boolean isEnabled(String worldName, ProtectionFlag flag) {
        Map<ProtectionFlag, Boolean> flags = worlds.get(normalize(worldName));
        return flags != null && flags.getOrDefault(flag, false);
    }

    public void setFlag(String worldName, ProtectionFlag flag, boolean enabled) {
        Map<ProtectionFlag, Boolean> flags = worlds.computeIfAbsent(normalize(worldName), ignored -> new EnumMap<>(ProtectionFlag.class));
        flags.put(flag, enabled);
        save();
    }

    public void protect(String worldName) {
        Map<ProtectionFlag, Boolean> flags = worlds.computeIfAbsent(normalize(worldName), ignored -> new EnumMap<>(ProtectionFlag.class));
        for (ProtectionFlag flag : ProtectionFlag.values()) flags.put(flag, false);
        for (ProtectionFlag flag : new ProtectionFlag[]{
                ProtectionFlag.BLOCK_BREAK, ProtectionFlag.BLOCK_PLACE, ProtectionFlag.EXPLOSIONS,
                ProtectionFlag.FIRE, ProtectionFlag.MOB_GRIEF, ProtectionFlag.INTERACT,
                ProtectionFlag.CONTAINER, ProtectionFlag.BUCKET, ProtectionFlag.ITEM_FRAME,
                ProtectionFlag.PISTONS, ProtectionFlag.LIQUID_FLOW
        }) flags.put(flag, true);
        save();
    }

    public void unprotect(String worldName) {
        Map<ProtectionFlag, Boolean> flags = worlds.computeIfAbsent(normalize(worldName), ignored -> new EnumMap<>(ProtectionFlag.class));
        for (ProtectionFlag flag : ProtectionFlag.values()) flags.put(flag, false);
        save();
    }

    public Map<ProtectionFlag, Boolean> flags(String worldName) {
        Map<ProtectionFlag, Boolean> values = worlds.get(normalize(worldName));
        Map<ProtectionFlag, Boolean> copy = new EnumMap<>(ProtectionFlag.class);
        if (values != null) copy.putAll(values);
        for (ProtectionFlag flag : ProtectionFlag.values()) copy.putIfAbsent(flag, false);
        return copy;
    }

    public Map<String, Map<ProtectionFlag, Boolean>> all() { return Map.copyOf(worlds); }

    private static String normalize(String value) { return value.toLowerCase(Locale.ROOT); }
}
