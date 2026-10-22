package Unbound.unboundProtection;

import Unbound.unboundProtection.command.ProtectionCommand;
import Unbound.unboundProtection.listener.ProtectionListener;
import Unbound.unboundProtection.region.RegionManager;
import Unbound.unboundProtection.region.WorldProtectionManager;
import Unbound.unboundProtection.selection.SelectionManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class UnboundProtection extends JavaPlugin {
    private RegionManager regionManager;
    private WorldProtectionManager worldProtectionManager;
    private SelectionManager selectionManager;
    private NamespacedKey wandKey;
    private final Set<UUID> bypassPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        wandKey = new NamespacedKey(this, "region_wand");
        selectionManager = new SelectionManager();
        regionManager = new RegionManager(this);
        regionManager.load();
        worldProtectionManager = new WorldProtectionManager(this);
        worldProtectionManager.load();

        ProtectionCommand command = new ProtectionCommand(this);
        if (getCommand("up") == null) {
            getLogger().severe("No se encontro el comando /up en plugin.yml. Desactivando plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getCommand("up").setExecutor(command);
        getCommand("up").setTabCompleter(command);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getLogger().info("UnboundProtection activo: " + regionManager.all().size() + " regiones cargadas.");
    }

    @Override
    public void onDisable() {
        if (regionManager != null) regionManager.save();
        if (worldProtectionManager != null) worldProtectionManager.save();
    }

    public RegionManager regions() { return regionManager; }
    public WorldProtectionManager worlds() { return worldProtectionManager; }
    public SelectionManager selections() { return selectionManager; }
    public NamespacedKey wandKey() { return wandKey; }
    public boolean isBypassing(Player player) {
        return player.hasPermission("unboundprotection.bypass") && bypassPlayers.contains(player.getUniqueId());
    }

    public void clearBypass(Player player) { bypassPlayers.remove(player.getUniqueId()); }
    public boolean toggleBypass(Player player) {
        UUID id = player.getUniqueId();
        if (bypassPlayers.remove(id)) return false;
        bypassPlayers.add(id);
        return true;
    }

    public String message(String message) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages.prefix", "&8[&bUnboundProtection&8] &r") + message);
    }
}
