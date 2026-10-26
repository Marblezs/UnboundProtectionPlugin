package Unbound.unboundProtection.command;

import Unbound.unboundProtection.UnboundProtection;
import Unbound.unboundProtection.model.ProtectedRegion;
import Unbound.unboundProtection.model.ProtectionFlag;
import Unbound.unboundProtection.region.RegionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ProtectionCommand implements CommandExecutor, TabCompleter {
    private final UnboundProtection plugin;

    public ProtectionCommand(UnboundProtection plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "wand" -> giveWand(sender);
            case "pos1" -> setPosition(sender, true);
            case "pos2" -> setPosition(sender, false);
            case "expandvert" -> expandVertical(sender);
            case "create" -> createRegion(sender, args);
            case "delete", "remove" -> deleteRegion(sender, args);
            case "list" -> listRegions(sender, args);
            case "info" -> infoRegion(sender, args);
            case "flag" -> setFlag(sender, args);
            case "flags" -> showFlags(sender);
            case "member" -> member(sender, args);
            case "world" -> world(sender, args);
            case "bypass" -> toggleBypass(sender);
            case "reload" -> reload(sender);
            default -> plugin.chat().send(sender, "unknown-command");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        plugin.chat().send(sender, "help-title");
        plugin.chat().send(sender, "help-wand");
        plugin.chat().send(sender, "help-position");
        plugin.chat().send(sender, "help-expand");
        plugin.chat().send(sender, "help-create");
        plugin.chat().send(sender, "help-delete");
        plugin.chat().send(sender, "help-list");
        plugin.chat().send(sender, "help-info");
        plugin.chat().send(sender, "help-flag");
        plugin.chat().send(sender, "help-flags");
        plugin.chat().send(sender, "help-member");
        plugin.chat().send(sender, "help-world");
        plugin.chat().send(sender, "help-bypass");
        plugin.chat().send(sender, "help-reload");
    }

    private void giveWand(CommandSender sender) {
        if (!require(sender, "unboundprotection.wand")) return;
        if (!(sender instanceof Player player)) {
            plugin.chat().send(sender, "player-only");
            return;
        }
        Material material = Material.matchMaterial(plugin.getConfig().getString("wand.material", "GOLDEN_AXE"));
        if (material == null || !material.isItem()) material = Material.GOLDEN_AXE;
        ItemStack wand = new ItemStack(material);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(plugin.getConfig().getString("wand.name", "&b&lUnbound &f- &eRegion Wand")));
            List<String> lore = plugin.getConfig().getStringList("wand.lore").stream().map(ProtectionCommand::color).toList();
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(plugin.wandKey(), PersistentDataType.BYTE, (byte) 1);
            wand.setItemMeta(meta);
        }
        player.getInventory().addItem(wand).values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        plugin.chat().send(player, "wand-given");
    }

    private void setPosition(CommandSender sender, boolean first) {
        if (!require(sender, "unboundprotection.wand")) return;
        if (!(sender instanceof Player player)) {
            plugin.chat().send(sender, "player-only");
            return;
        }
        Location location = player.getLocation().getBlock().getLocation();
        if (first) plugin.selections().setPos1(player.getUniqueId(), location);
        else plugin.selections().setPos2(player.getUniqueId(), location);
        sendSelection(player, first, location);
    }

    private void expandVertical(CommandSender sender) {
        if (!require(sender, "unboundprotection.wand")) return;
        if (!(sender instanceof Player player)) {
            plugin.chat().send(sender, "player-only");
            return;
        }
        UUID uuid = player.getUniqueId();
        Location first = plugin.selections().getPos1(uuid);
        Location second = plugin.selections().getPos2(uuid);
        if (first == null || second == null) {
            plugin.chat().send(player, "selection-expand-missing");
            return;
        }
        if (!first.getWorld().equals(second.getWorld())) {
            plugin.chat().send(player, "selection-world-different");
            return;
        }
        first.setY(first.getWorld().getMinHeight());
        second.setY(second.getWorld().getMaxHeight() - 1);
        plugin.selections().setPos1(uuid, first);
        plugin.selections().setPos2(uuid, second);
        plugin.chat().send(player, "selection-expanded", first.getWorld().getName());
    }

    private void createRegion(CommandSender sender, String[] args) {
        if (!require(sender, "unboundprotection.region.create")) return;
        if (!(sender instanceof Player player)) {
            plugin.chat().send(sender, "create-player-only");
            return;
        }
        if (args.length < 2) {
            plugin.chat().send(sender, "create-usage");
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z0-9_-]{1,32}")) {
            plugin.chat().send(sender, "region-name-invalid");
            return;
        }
        UUID uuid = player.getUniqueId();
        if (!plugin.selections().hasCompleteSelection(uuid)) {
            plugin.chat().send(sender, "selection-missing");
            return;
        }
        Location first = plugin.selections().getPos1(uuid);
        Location second = plugin.selections().getPos2(uuid);
        if (first.getWorld() == null || second.getWorld() == null || !first.getWorld().equals(second.getWorld())) {
            plugin.chat().send(sender, "selection-world-different");
            return;
        }
        if (args.length >= 3 && !args[2].equalsIgnoreCase(first.getWorld().getName())) {
            plugin.chat().send(sender, "selection-world-mismatch", first.getWorld().getName(), args[2]);
            return;
        }
        ProtectedRegion existing = plugin.regions().get(id);
        if (existing != null) {
            plugin.chat().send(sender, "region-exists");
            return;
        }
        int limit = plugin.getConfig().getInt("settings.max-regions-per-player", 0);
        if (limit > 0) {
            long count = plugin.regions().all().stream().filter(r -> uuid.equals(r.ownerId())).count();
            if (count >= limit) {
                plugin.chat().send(sender, "region-limit", String.valueOf(limit));
                return;
            }
        }
        ProtectedRegion region = new ProtectedRegion(id, first.getWorld().getName(),
                first.getBlockX(), first.getBlockY(), first.getBlockZ(),
                second.getBlockX(), second.getBlockY(), second.getBlockZ(), uuid, player.getName());
        if (!plugin.getConfig().getBoolean("settings.protect-by-default", true)) {
            for (ProtectionFlag flag : ProtectionFlag.values()) region.setFlag(flag, false);
        }
        long maxBlocks = plugin.getConfig().getLong("settings.max-selection-blocks", 0L);
        if (maxBlocks > 0 && region.volume() > maxBlocks) {
            plugin.chat().send(sender, "region-too-large", String.valueOf(region.volume()), String.valueOf(maxBlocks));
            return;
        }
        if (!plugin.regions().create(region)) {
            plugin.chat().send(sender, "region-create-failed");
            return;
        }
        plugin.selections().clear(uuid);
        plugin.chat().send(sender, "region-created", region.id(), region.worldName(), String.valueOf(region.volume()));
        plugin.chat().send(sender, "region-created-tip");
    }

    private void deleteRegion(CommandSender sender, String[] args) {
        if (!require(sender, "unboundprotection.region.delete")) return;
        if (args.length < 2) {
            plugin.chat().send(sender, "delete-usage");
            return;
        }
        ProtectedRegion region = plugin.regions().get(args[1]);
        if (region == null) {
            plugin.chat().send(sender, "region-not-found");
            return;
        }
        if (sender instanceof Player player && !player.hasPermission("unboundprotection.admin")
                && !player.getUniqueId().equals(region.ownerId())) {
            plugin.chat().send(sender, "region-delete-denied");
            return;
        }
        plugin.regions().delete(region.id());
        plugin.chat().send(sender, "region-deleted", region.id());
    }

    private void listRegions(CommandSender sender, String[] args) {
        if (!require(sender, "unboundprotection.region.list")) return;
        List<ProtectedRegion> regions;
        if (args.length >= 2) {
            World world = RegionManager.findWorld(args[1]);
            String name = world == null ? args[1] : world.getName();
            regions = plugin.regions().inWorld(name);
        } else {
            regions = new ArrayList<>(plugin.regions().all());
        }
        plugin.chat().send(sender, "region-list-title", String.valueOf(regions.size()));
        if (regions.isEmpty()) {
            plugin.chat().send(sender, "region-list-empty");
            return;
        }
        for (ProtectedRegion region : regions) {
            plugin.chat().send(sender, "region-list-entry", region.id(), region.worldName(), RegionManager.ownerDisplay(region));
        }
    }

    private void infoRegion(CommandSender sender, String[] args) {
        if (!require(sender, "unboundprotection.region.info")) return;
        List<ProtectedRegion> found;
        if (args.length >= 2 && !args[1].equalsIgnoreCase("here")) {
            ProtectedRegion region = plugin.regions().get(args[1]);
            if (region == null) {
                plugin.chat().send(sender, "region-not-found");
                return;
            }
            found = List.of(region);
        } else if (sender instanceof Player player) {
            found = plugin.regions().at(player.getLocation());
        } else {
            plugin.chat().send(sender, "region-info-console");
            return;
        }
        if (found.isEmpty()) {
            plugin.chat().send(sender, "region-not-here");
            return;
        }
        for (ProtectedRegion region : found) {
            plugin.chat().send(sender, "region-info-title", region.id(), region.worldName());
            plugin.chat().send(sender, "region-info-owner", RegionManager.ownerDisplay(region), String.valueOf(region.volume()));
            plugin.chat().send(sender, "region-info-bounds", String.valueOf(region.minX()), String.valueOf(region.minY()),
                    String.valueOf(region.minZ()), String.valueOf(region.maxX()), String.valueOf(region.maxY()), String.valueOf(region.maxZ()));
            String enabled = Arrays.stream(ProtectionFlag.values()).filter(region::flag).map(ProtectionFlag::key).collect(Collectors.joining(", "));
            plugin.chat().send(sender, "region-info-flags", enabled.isEmpty() ? "ninguna" : enabled);
            plugin.chat().send(sender, "region-info-members", String.valueOf(region.members().size()));
        }
    }

    private void setFlag(CommandSender sender, String[] args) {
        if (!require(sender, "unboundprotection.region.flag")) return;
        if (args.length < 4) {
            plugin.chat().send(sender, "region-flag-usage");
            return;
        }
        ProtectedRegion region;
        if (args[1].equalsIgnoreCase("here")) {
            if (!(sender instanceof Player player)) {
                plugin.chat().send(sender, "region-flag-console");
                return;
            }
            List<ProtectedRegion> atLocation = plugin.regions().at(player.getLocation());
            if (atLocation.size() > 1) {
                plugin.chat().send(sender, "region-overlap");
                return;
            }
            region = atLocation.isEmpty() ? null : atLocation.get(0);
        } else {
            region = plugin.regions().get(args[1]);
        }
        if (region == null) {
            plugin.chat().send(sender, "region-flag-not-found");
            return;
        }
        ProtectionFlag flag = ProtectionFlag.fromKey(args[2]).orElse(null);
        if (flag == null) {
            plugin.chat().send(sender, "flag-unknown");
            return;
        }
        String value = args[3].toLowerCase(Locale.ROOT);
        boolean enabled;
        if (Set.of("on", "true", "yes", "si", "sí", "activar", "enable").contains(value)) enabled = true;
        else if (Set.of("off", "false", "no", "desactivar", "disable").contains(value)) enabled = false;
        else {
            plugin.chat().send(sender, "flag-invalid-value");
            return;
        }
        if (sender instanceof Player player && !player.hasPermission("unboundprotection.admin")
                && !player.getUniqueId().equals(region.ownerId())) {
            plugin.chat().send(sender, "region-flag-denied");
            return;
        }
        region.setFlag(flag, enabled);
        plugin.regions().save();
        plugin.chat().send(sender, "region-flag-set", flag.key(), region.id(), enabled ? "&2ON" : "&cOFF");
    }

    private void showFlags(CommandSender sender) {
        plugin.chat().send(sender, "flags-title");
        for (ProtectionFlag flag : ProtectionFlag.values()) {
            plugin.chat().send(sender, "flag-entry", flag.key(), flag.description(), flag.enabledByDefault() ? "ON" : "OFF");
        }
    }

    private void member(CommandSender sender, String[] args) {
        if (!require(sender, "unboundprotection.region.members")) return;
        if (args.length < 4 || !(args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))) {
            plugin.chat().send(sender, "member-usage");
            return;
        }
        ProtectedRegion region = plugin.regions().get(args[2]);
        if (region == null) {
            plugin.chat().send(sender, "region-not-found");
            return;
        }
        if (sender instanceof Player player && !player.hasPermission("unboundprotection.admin")
                && !player.getUniqueId().equals(region.ownerId())) {
            plugin.chat().send(sender, "member-manage-denied");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);
        if (target.getUniqueId() == null) {
            plugin.chat().send(sender, "player-unknown");
            return;
        }
        if (region.ownerId() != null && region.ownerId().equals(target.getUniqueId())) {
            plugin.chat().send(sender, "owner-already-has-access");
            return;
        }
        boolean adding = args[1].equalsIgnoreCase("add");
        boolean changed = adding ? region.members().add(target.getUniqueId()) : region.members().remove(target.getUniqueId());
        if (!changed) {
            plugin.chat().send(sender, adding ? "member-already" : "member-not-member");
            return;
        }
        plugin.regions().save();
        plugin.chat().send(sender, adding ? "member-added" : "member-removed", args[3], region.id());
    }

    private void world(CommandSender sender, String[] args) {
        if (!require(sender, "unboundprotection.world.manage")) return;
        if (args.length < 2) {
            plugin.chat().send(sender, "world-usage");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("list")) {
            plugin.chat().send(sender, "world-list-title");
            for (World world : Bukkit.getWorlds()) {
                long active = plugin.worlds().flags(world.getName()).values().stream().filter(Boolean::booleanValue).count();
                plugin.chat().send(sender, "world-list-entry", world.getName(), String.valueOf(active));
            }
            return;
        }
        if (args.length < 3) {
            plugin.chat().send(sender, "world-action-usage");
            return;
        }
        World world = RegionManager.findWorld(args[2]);
        if (world == null) {
            plugin.chat().send(sender, "world-not-loaded");
            return;
        }
        switch (action) {
            case "protect" -> {
                plugin.worlds().protect(world.getName());
                plugin.chat().send(sender, "world-protected", world.getName());
            }
            case "unprotect" -> {
                plugin.worlds().unprotect(world.getName());
                plugin.chat().send(sender, "world-unprotected", world.getName());
            }
            case "flags" -> {
                Map<ProtectionFlag, Boolean> flags = plugin.worlds().flags(world.getName());
                String active = flags.entrySet().stream().filter(Map.Entry::getValue).map(entry -> entry.getKey().key()).collect(Collectors.joining(", "));
                plugin.chat().send(sender, "world-flags-title", world.getName());
                plugin.chat().send(sender, "world-flags-active", active.isEmpty() ? "ninguna" : active);
            }
            case "flag" -> {
                if (args.length < 5) {
                    plugin.chat().send(sender, "world-flag-usage");
                    return;
                }
                ProtectionFlag flag = ProtectionFlag.fromKey(args[3]).orElse(null);
                if (flag == null) {
                    plugin.chat().send(sender, "flag-unknown");
                    return;
                }
                String value = args[4].toLowerCase(Locale.ROOT);
                boolean enabled;
                if (Set.of("on", "true", "yes", "si", "sí", "activar", "enable").contains(value)) enabled = true;
                else if (Set.of("off", "false", "no", "desactivar", "disable").contains(value)) enabled = false;
                else {
                    plugin.chat().send(sender, "flag-invalid-value");
                    return;
                }
                plugin.worlds().setFlag(world.getName(), flag, enabled);
                plugin.chat().send(sender, "world-flag-set", flag.key(), world.getName(), enabled ? "&2ON" : "&cOFF");
            }
            default -> plugin.chat().send(sender, "world-action-unknown");
        }
    }

    private void toggleBypass(CommandSender sender) {
        if (!require(sender, "unboundprotection.bypass")) return;
        if (!(sender instanceof Player player)) {
            plugin.chat().send(sender, "player-only");
            return;
        }
        boolean bypass = plugin.toggleBypass(player);
        plugin.chat().send(sender, bypass ? "bypass-enabled" : "bypass-disabled");
    }

    private void reload(CommandSender sender) {
        if (!require(sender, "unboundprotection.reload")) return;
        plugin.reloadConfig();
        plugin.mergeDefaultConfig();
        plugin.regions().load();
        plugin.worlds().load();
        plugin.chat().send(sender, "reload-complete");
    }

    private boolean require(CommandSender sender, String permission) {
        if (sender.hasPermission("unboundprotection.admin") || sender.hasPermission(permission)) return true;
        plugin.chat().send(sender, "no-permission", permission);
        return false;
    }

    private void sendSelection(Player player, boolean first, Location location) {
        plugin.chat().send(player, "selection-marked", first ? "1" : "2", String.valueOf(location.getBlockX()),
                String.valueOf(location.getBlockY()), String.valueOf(location.getBlockZ()), location.getWorld().getName());
    }

    private static String color(String text) { return ChatColor.translateAlternateColorCodes('&', text); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> candidates = new ArrayList<>();
        if (args.length == 1) {
            addIfAllowed(candidates, sender, "wand", "unboundprotection.wand");
            addIfAllowed(candidates, sender, "pos1", "unboundprotection.wand");
            addIfAllowed(candidates, sender, "pos2", "unboundprotection.wand");
            addIfAllowed(candidates, sender, "expandvert", "unboundprotection.wand");
            addIfAllowed(candidates, sender, "create", "unboundprotection.region.create");
            addIfAllowed(candidates, sender, "delete", "unboundprotection.region.delete");
            addIfAllowed(candidates, sender, "list", "unboundprotection.region.list");
            addIfAllowed(candidates, sender, "info", "unboundprotection.region.info");
            addIfAllowed(candidates, sender, "flag", "unboundprotection.region.flag");
            addIfAllowed(candidates, sender, "flags", "unboundprotection.region.flag");
            addIfAllowed(candidates, sender, "member", "unboundprotection.region.members");
            addIfAllowed(candidates, sender, "world", "unboundprotection.world.manage");
            addIfAllowed(candidates, sender, "bypass", "unboundprotection.bypass");
            addIfAllowed(candidates, sender, "reload", "unboundprotection.reload");
            candidates.add("help");
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "delete", "remove", "info" -> candidates.addAll(plugin.regions().all().stream().map(ProtectedRegion::id).toList());
                case "list" -> candidates.addAll(Bukkit.getWorlds().stream().map(World::getName).toList());
                case "flag" -> { candidates.add("here"); candidates.addAll(plugin.regions().all().stream().map(ProtectedRegion::id).toList()); }
                case "member" -> { candidates.add("add"); candidates.add("remove"); }
                case "world" -> { candidates.add("list"); candidates.add("protect"); candidates.add("unprotect"); candidates.add("flags"); candidates.add("flag"); }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("create")) {
                candidates.addAll(Bukkit.getWorlds().stream().map(World::getName).toList());
            } else if (args[0].equalsIgnoreCase("flag")) {
                candidates.addAll(Arrays.stream(ProtectionFlag.values()).map(ProtectionFlag::key).toList());
            } else if (args[0].equalsIgnoreCase("member")) {
                candidates.addAll(plugin.regions().all().stream().map(ProtectedRegion::id).toList());
            } else if (args[0].equalsIgnoreCase("world")) {
                if (args[1].equalsIgnoreCase("flag")) candidates.addAll(Bukkit.getWorlds().stream().map(World::getName).toList());
                else if (!args[1].equalsIgnoreCase("list")) candidates.addAll(Bukkit.getWorlds().stream().map(World::getName).toList());
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("flag")) {
                candidates.addAll(List.of("on", "off"));
            } else if (args[0].equalsIgnoreCase("member")) {
                candidates.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            } else if (args[0].equalsIgnoreCase("world") && args[1].equalsIgnoreCase("flag")) {
                candidates.addAll(Arrays.stream(ProtectionFlag.values()).map(ProtectionFlag::key).toList());
            }
        } else if (args.length == 5 && args[0].equalsIgnoreCase("world") && args[1].equalsIgnoreCase("flag")) {
            candidates.addAll(List.of("on", "off"));
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return candidates.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix)).distinct().sorted().toList();
    }

    private void addIfAllowed(List<String> list, CommandSender sender, String value, String permission) {
        if (sender.hasPermission("unboundprotection.admin") || sender.hasPermission(permission)) list.add(value);
    }
}
