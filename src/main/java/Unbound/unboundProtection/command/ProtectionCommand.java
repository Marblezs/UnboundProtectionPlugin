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
            default -> sender.sendMessage(plugin.message("&cComando desconocido. Usa &f/up help&c."));
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.message("&bComandos disponibles:"));
        sender.sendMessage(color("&e/up wand &7- Obtener la wand de seleccion."));
        sender.sendMessage(color("&e/up pos1|pos2 &7- Marcar una posicion donde estas."));
        sender.sendMessage(color("&e/up expandvert &7- Expandir la seleccion hasta toda la altura del mundo."));
        sender.sendMessage(color("&e/up create <nombre> [mundo] &7- Crear una region con la seleccion."));
        sender.sendMessage(color("&e/up delete <region> &7- Eliminar una region."));
        sender.sendMessage(color("&e/up list [mundo] &7- Listar regiones."));
        sender.sendMessage(color("&e/up info [region] &7- Ver los detalles de una region."));
        sender.sendMessage(color("&e/up flag <region|here> <flag> <on|off> &7- Ajustar protecciones."));
        sender.sendMessage(color("&e/up flags &7- Mostrar todas las flags."));
        sender.sendMessage(color("&e/up member <add|remove> <region> <jugador> &7- Gestionar miembros."));
        sender.sendMessage(color("&e/up world <protect|unprotect|flag|flags|list> ... &7- Proteger mundos completos."));
        sender.sendMessage(color("&e/up bypass &7- Alternar tu bypass (requiere permiso)."));
        sender.sendMessage(color("&e/up reload &7- Recargar archivos."));
    }

    private void giveWand(CommandSender sender) {
        if (!require(sender, "unboundprotection.wand")) return;
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.message("&cEste comando solo puede usarlo un jugador."));
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
        player.sendMessage(plugin.message("&aWand entregada. Click izquierdo: pos1; click derecho: pos2."));
    }

    private void setPosition(CommandSender sender, boolean first) {
        if (!require(sender, "unboundprotection.wand")) return;
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.message("&cEste comando solo puede usarlo un jugador."));
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
            sender.sendMessage(plugin.message("&cEste comando solo puede usarlo un jugador."));
            return;
        }
        UUID uuid = player.getUniqueId();
        Location first = plugin.selections().getPos1(uuid);
        Location second = plugin.selections().getPos2(uuid);
        if (first == null || second == null) {
            player.sendMessage(plugin.message("&cMarca pos1 y pos2 antes de expandir."));
            return;
        }
        if (!first.getWorld().equals(second.getWorld())) {
            player.sendMessage(plugin.message("&cLas dos posiciones deben estar en el mismo mundo."));
            return;
        }
        first.setY(first.getWorld().getMinHeight());
        second.setY(second.getWorld().getMaxHeight() - 1);
        plugin.selections().setPos1(uuid, first);
        plugin.selections().setPos2(uuid, second);
        player.sendMessage(plugin.message("&aSeleccion expandida verticalmente en &f" + first.getWorld().getName() + "&a."));
    }

    private void createRegion(CommandSender sender, String[] args) {
        if (!require(sender, "unboundprotection.region.create")) return;
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.message("&cLa creacion requiere seleccionar pos1 y pos2 como jugador."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.message("&cUso: /up create <nombre> [mundo]"));
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z0-9_-]{1,32}")) {
            sender.sendMessage(plugin.message("&cNombre invalido. Usa hasta 32 letras, numeros, guion o guion bajo."));
            return;
        }
        UUID uuid = player.getUniqueId();
        if (!plugin.selections().hasCompleteSelection(uuid)) {
            sender.sendMessage(plugin.message("&cPrimero marca pos1 y pos2 con &f/up wand&c."));
            return;
        }
        Location first = plugin.selections().getPos1(uuid);
        Location second = plugin.selections().getPos2(uuid);
        if (first.getWorld() == null || second.getWorld() == null || !first.getWorld().equals(second.getWorld())) {
            sender.sendMessage(plugin.message("&cLas dos posiciones deben estar en el mismo mundo."));
            return;
        }
        if (args.length >= 3 && !args[2].equalsIgnoreCase(first.getWorld().getName())) {
            sender.sendMessage(plugin.message("&cLa seleccion pertenece al mundo &f" + first.getWorld().getName() + "&c, no a &f" + args[2] + "&c."));
            return;
        }
        ProtectedRegion existing = plugin.regions().get(id);
        if (existing != null) {
            sender.sendMessage(plugin.message("&cYa existe una region con ese nombre."));
            return;
        }
        int limit = plugin.getConfig().getInt("settings.max-regions-per-player", 0);
        if (limit > 0) {
            long count = plugin.regions().all().stream().filter(r -> uuid.equals(r.ownerId())).count();
            if (count >= limit) {
                sender.sendMessage(plugin.message("&cLlegaste al limite de regiones (&f" + limit + "&c)."));
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
            sender.sendMessage(plugin.message("&cLa region ocupa &f" + region.volume() + "&c bloques; el maximo es &f" + maxBlocks + "&c."));
            return;
        }
        if (!plugin.regions().create(region)) {
            sender.sendMessage(plugin.message("&cNo se pudo crear la region."));
            return;
        }
        plugin.selections().clear(uuid);
        sender.sendMessage(plugin.message("&aRegion &f" + region.id() + " &acreada en el mundo &f" + region.worldName() + "&a. Bloques: &f" + region.volume() + "&a."));
        sender.sendMessage(plugin.message("&7Por defecto protege construccion, contenedores, fuego, fluidos, pistones y explosiones. Usa &f/up flags&7."));
    }

    private void deleteRegion(CommandSender sender, String[] args) {
        if (!require(sender, "unboundprotection.region.delete")) return;
        if (args.length < 2) {
            sender.sendMessage(plugin.message("&cUso: /up delete <region>"));
            return;
        }
        ProtectedRegion region = plugin.regions().get(args[1]);
        if (region == null) {
            sender.sendMessage(plugin.message("&cNo existe esa region."));
            return;
        }
        if (sender instanceof Player player && !player.hasPermission("unboundprotection.admin")
                && !player.getUniqueId().equals(region.ownerId())) {
            sender.sendMessage(plugin.message("&cSolo el propietario o un administrador puede eliminarla."));
            return;
        }
        plugin.regions().delete(region.id());
        sender.sendMessage(plugin.message("&aRegion &f" + region.id() + " &aeliminada."));
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
        sender.sendMessage(plugin.message("&bRegiones (&f" + regions.size() + "&b):"));
        if (regions.isEmpty()) {
            sender.sendMessage(color("&7No hay regiones guardadas para este filtro."));
            return;
        }
        for (ProtectedRegion region : regions) {
            sender.sendMessage(color("&8- &e" + region.id() + " &7| &f" + region.worldName() + " &7| propietario: &f" + RegionManager.ownerDisplay(region)));
        }
    }

    private void infoRegion(CommandSender sender, String[] args) {
        if (!require(sender, "unboundprotection.region.info")) return;
        List<ProtectedRegion> found;
        if (args.length >= 2 && !args[1].equalsIgnoreCase("here")) {
            ProtectedRegion region = plugin.regions().get(args[1]);
            if (region == null) {
                sender.sendMessage(plugin.message("&cNo existe esa region."));
                return;
            }
            found = List.of(region);
        } else if (sender instanceof Player player) {
            found = plugin.regions().at(player.getLocation());
        } else {
            sender.sendMessage(plugin.message("&cDesde consola especifica una region: /up info <region>"));
            return;
        }
        if (found.isEmpty()) {
            sender.sendMessage(plugin.message("&7No estas dentro de ninguna region protegida."));
            return;
        }
        for (ProtectedRegion region : found) {
            sender.sendMessage(color("&bRegion &f" + region.id() + " &7(mundo: &f" + region.worldName() + "&7)"));
            sender.sendMessage(color("&7Propietario: &f" + RegionManager.ownerDisplay(region) + " &8| &7Volumen: &f" + region.volume()));
            sender.sendMessage(color("&7Min: &f" + region.minX() + ", " + region.minY() + ", " + region.minZ()
                    + " &7Max: &f" + region.maxX() + ", " + region.maxY() + ", " + region.maxZ()));
            String enabled = Arrays.stream(ProtectionFlag.values()).filter(region::flag).map(ProtectionFlag::key).collect(Collectors.joining(", "));
            sender.sendMessage(color("&7Flags activas: &f" + (enabled.isEmpty() ? "ninguna" : enabled)));
            sender.sendMessage(color("&7Miembros: &f" + region.members().size()));
        }
    }

    private void setFlag(CommandSender sender, String[] args) {
        if (!require(sender, "unboundprotection.region.flag")) return;
        if (args.length < 4) {
            sender.sendMessage(plugin.message("&cUso: /up flag <region|here> <flag> <on|off>"));
            return;
        }
        ProtectedRegion region;
        if (args[1].equalsIgnoreCase("here")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.message("&cUsa un nombre de region desde consola."));
                return;
            }
            List<ProtectedRegion> atLocation = plugin.regions().at(player.getLocation());
            if (atLocation.size() > 1) {
                sender.sendMessage(plugin.message("&cHay varias regiones superpuestas. Especifica el nombre en /up flag <region> <flag> <on|off>."));
                return;
            }
            region = atLocation.isEmpty() ? null : atLocation.get(0);
        } else {
            region = plugin.regions().get(args[1]);
        }
        if (region == null) {
            sender.sendMessage(plugin.message("&cNo se encontro la region. Si hay varias superpuestas, especifica su nombre."));
            return;
        }
        ProtectionFlag flag = ProtectionFlag.fromKey(args[2]).orElse(null);
        if (flag == null) {
            sender.sendMessage(plugin.message("&cFlag desconocida. Usa &f/up flags&c."));
            return;
        }
        String value = args[3].toLowerCase(Locale.ROOT);
        boolean enabled;
        if (Set.of("on", "true", "yes", "si", "sí", "activar", "enable").contains(value)) enabled = true;
        else if (Set.of("off", "false", "no", "desactivar", "disable").contains(value)) enabled = false;
        else {
            sender.sendMessage(plugin.message("&cValor invalido. Usa &fon &co &foff&c."));
            return;
        }
        if (sender instanceof Player player && !player.hasPermission("unboundprotection.admin")
                && !player.getUniqueId().equals(region.ownerId())) {
            sender.sendMessage(plugin.message("&cSolo el propietario o un administrador puede modificarla."));
            return;
        }
        region.setFlag(flag, enabled);
        plugin.regions().save();
        sender.sendMessage(plugin.message("&aFlag &f" + flag.key() + " &ade &f" + region.id() + " &aestablecida en " + (enabled ? "&2ON" : "&cOFF") + "&a."));
    }

    private void showFlags(CommandSender sender) {
        sender.sendMessage(plugin.message("&bFlags disponibles (ON significa que se bloquea la accion):"));
        for (ProtectionFlag flag : ProtectionFlag.values()) {
            sender.sendMessage(color("&e" + flag.key() + " &7- " + flag.description() + " &8(default: " + (flag.enabledByDefault() ? "ON" : "OFF") + ")"));
        }
    }

    private void member(CommandSender sender, String[] args) {
        if (!require(sender, "unboundprotection.region.members")) return;
        if (args.length < 4 || !(args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))) {
            sender.sendMessage(plugin.message("&cUso: /up member <add|remove> <region> <jugador>"));
            return;
        }
        ProtectedRegion region = plugin.regions().get(args[2]);
        if (region == null) {
            sender.sendMessage(plugin.message("&cNo existe esa region."));
            return;
        }
        if (sender instanceof Player player && !player.hasPermission("unboundprotection.admin")
                && !player.getUniqueId().equals(region.ownerId())) {
            sender.sendMessage(plugin.message("&cSolo el propietario o un administrador puede gestionar miembros."));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);
        if (target.getUniqueId() == null) {
            sender.sendMessage(plugin.message("&cNo se pudo identificar ese jugador."));
            return;
        }
        if (region.ownerId() != null && region.ownerId().equals(target.getUniqueId())) {
            sender.sendMessage(plugin.message("&cEl propietario ya tiene acceso a la region."));
            return;
        }
        boolean adding = args[1].equalsIgnoreCase("add");
        boolean changed = adding ? region.members().add(target.getUniqueId()) : region.members().remove(target.getUniqueId());
        if (!changed) {
            sender.sendMessage(plugin.message(adding ? "&eEse jugador ya es miembro." : "&eEse jugador no era miembro."));
            return;
        }
        plugin.regions().save();
        sender.sendMessage(plugin.message("&a" + (adding ? "Agregado" : "Quitado") + " &f" + args[3] + " &a" + (adding ? "a" : "de") + " la region &f" + region.id() + "&a."));
    }

    private void world(CommandSender sender, String[] args) {
        if (!require(sender, "unboundprotection.world.manage")) return;
        if (args.length < 2) {
            sender.sendMessage(plugin.message("&cUso: /up world <list|protect|unprotect|flags|flag> ..."));
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("list")) {
            sender.sendMessage(plugin.message("&bMundos cargados por Bukkit (incluye mundos cargados por Multiverse):"));
            for (World world : Bukkit.getWorlds()) {
                long active = plugin.worlds().flags(world.getName()).values().stream().filter(Boolean::booleanValue).count();
                sender.sendMessage(color("&8- &e" + world.getName() + " &7| flags globales activas: &f" + active));
            }
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.message("&cUso: /up world <protect|unprotect|flags> <mundo> o /up world flag <mundo> <flag> <on|off>"));
            return;
        }
        World world = RegionManager.findWorld(args[2]);
        if (world == null) {
            sender.sendMessage(plugin.message("&cEse mundo no esta cargado por Bukkit. Revisa el nombre con &f/up world list&c."));
            return;
        }
        switch (action) {
            case "protect" -> {
                plugin.worlds().protect(world.getName());
                sender.sendMessage(plugin.message("&aProteccion global activada para &f" + world.getName() + "&a. Se bloquearon construccion, interacciones, fuego, fluidos, pistones y explosiones."));
            }
            case "unprotect" -> {
                plugin.worlds().unprotect(world.getName());
                sender.sendMessage(plugin.message("&eSe desactivaron las flags globales de &f" + world.getName() + "&e. Las regiones individuales siguen funcionando."));
            }
            case "flags" -> {
                Map<ProtectionFlag, Boolean> flags = plugin.worlds().flags(world.getName());
                String active = flags.entrySet().stream().filter(Map.Entry::getValue).map(entry -> entry.getKey().key()).collect(Collectors.joining(", "));
                sender.sendMessage(plugin.message("&bFlags globales de &f" + world.getName() + "&b:"));
                sender.sendMessage(color("&7Activas: &f" + (active.isEmpty() ? "ninguna" : active)));
            }
            case "flag" -> {
                if (args.length < 5) {
                    sender.sendMessage(plugin.message("&cUso: /up world flag <mundo> <flag> <on|off>"));
                    return;
                }
                ProtectionFlag flag = ProtectionFlag.fromKey(args[3]).orElse(null);
                if (flag == null) {
                    sender.sendMessage(plugin.message("&cFlag desconocida. Usa &f/up flags&c."));
                    return;
                }
                String value = args[4].toLowerCase(Locale.ROOT);
                boolean enabled;
                if (Set.of("on", "true", "yes", "si", "sí", "activar", "enable").contains(value)) enabled = true;
                else if (Set.of("off", "false", "no", "desactivar", "disable").contains(value)) enabled = false;
                else {
                    sender.sendMessage(plugin.message("&cValor invalido. Usa &fon &co &foff&c."));
                    return;
                }
                plugin.worlds().setFlag(world.getName(), flag, enabled);
                sender.sendMessage(plugin.message("&aFlag global &f" + flag.key() + " &ade &f" + world.getName() + " &aestablecida en " + (enabled ? "&2ON" : "&cOFF") + "&a."));
            }
            default -> sender.sendMessage(plugin.message("&cAccion desconocida. Usa &f/up world list&c."));
        }
    }

    private void toggleBypass(CommandSender sender) {
        if (!require(sender, "unboundprotection.bypass")) return;
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.message("&cEste comando solo puede usarlo un jugador."));
            return;
        }
        boolean bypass = plugin.toggleBypass(player);
        sender.sendMessage(plugin.message("&7Bypass: " + (bypass ? "&aACTIVADO" : "&cDESACTIVADO")));
    }

    private void reload(CommandSender sender) {
        if (!require(sender, "unboundprotection.reload")) return;
        plugin.reloadConfig();
        plugin.regions().load();
        plugin.worlds().load();
        sender.sendMessage(plugin.message("&aConfiguracion, regiones y protecciones de mundos recargadas."));
    }

    private boolean require(CommandSender sender, String permission) {
        if (sender.hasPermission("unboundprotection.admin") || sender.hasPermission(permission)) return true;
        sender.sendMessage(plugin.message("&cNo tienes permiso: &f" + permission));
        return false;
    }

    private void sendSelection(Player player, boolean first, Location location) {
        player.sendMessage(plugin.message("&aPosicion " + (first ? "&f1" : "&f2") + " &amarcada: &f" + location.getBlockX() + ", "
                + location.getBlockY() + ", " + location.getBlockZ() + " &7(mundo: " + location.getWorld().getName() + ")"));
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
