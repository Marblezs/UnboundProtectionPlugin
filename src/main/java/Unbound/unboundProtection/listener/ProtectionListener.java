package Unbound.unboundProtection.listener;

import Unbound.unboundProtection.UnboundProtection;
import Unbound.unboundProtection.model.ProtectedRegion;
import Unbound.unboundProtection.model.ProtectionFlag;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class ProtectionListener implements Listener {
    private final UnboundProtection plugin;

    public ProtectionListener(UnboundProtection plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onWandUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().getPersistentDataContainer()
                .has(plugin.wandKey(), PersistentDataType.BYTE)) return;
        if (!player.hasPermission("unboundprotection.wand") && !player.hasPermission("unboundprotection.admin")) {
            player.sendMessage(plugin.message("&cNo tienes permiso para usar la wand."));
            event.setCancelled(true);
            return;
        }
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return;
        Location target;
        if (action == Action.RIGHT_CLICK_AIR || event.getClickedBlock() == null) {
            target = player.getLocation().getBlock().getLocation();
        } else {
            target = event.getClickedBlock().getLocation();
        }
        if (action == Action.LEFT_CLICK_BLOCK) {
            plugin.selections().setPos1(player.getUniqueId(), target);
            sendPosition(player, "1", target);
        } else {
            plugin.selections().setPos2(player.getUniqueId(), target);
            sendPosition(player, "2", target);
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (blocked(event.getBlock().getLocation(), ProtectionFlag.BLOCK_BREAK, event.getPlayer(), true)) {
            event.setCancelled(true);
            deny(event.getPlayer(), "romper bloques");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (blocked(event.getBlockPlaced().getLocation(), ProtectionFlag.BLOCK_PLACE, event.getPlayer(), true)) {
            event.setCancelled(true);
            deny(event.getPlayer(), "colocar bloques");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null) return;
        Location location = event.getClickedBlock().getLocation();
        if (blocked(location, ProtectionFlag.INTERACT, player, true)) {
            event.setCancelled(true);
            deny(player, "interactuar con bloques");
            return;
        }
        if (event.getClickedBlock().getState() instanceof InventoryHolder
                && blocked(location, ProtectionFlag.CONTAINER, player, true)) {
            event.setCancelled(true);
            deny(player, "abrir contenedores");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity instanceof ItemFrame || entity instanceof GlowItemFrame || entity instanceof ArmorStand) {
            if (blocked(entity.getLocation(), ProtectionFlag.ITEM_FRAME, event.getPlayer(), true)) {
                event.setCancelled(true);
                deny(event.getPlayer(), "modificar marcos o soportes de armadura");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingExplosion(HangingBreakEvent event) {
        if (event.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION
                && environmentBlocked(event.getEntity().getLocation(), ProtectionFlag.EXPLOSIONS, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player player
                && blocked(event.getEntity().getLocation(), ProtectionFlag.ITEM_FRAME, player, true)) {
            event.setCancelled(true);
            deny(player, "romper marcos");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (blocked(event.getEntity().getLocation(), ProtectionFlag.ITEM_FRAME, event.getPlayer(), true)) {
            event.setCancelled(true);
            deny(event.getPlayer(), "colocar marcos");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Location target = event.getBlockClicked().getRelative(event.getBlockFace()).getLocation();
        if (blocked(target, ProtectionFlag.BUCKET, event.getPlayer(), true)) {
            event.setCancelled(true);
            deny(event.getPlayer(), "usar cubos");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (blocked(event.getBlockClicked().getLocation(), ProtectionFlag.BUCKET, event.getPlayer(), true)) {
            event.setCancelled(true);
            deny(event.getPlayer(), "usar cubos");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = getPlayerDamager(event.getDamager());
        if (attacker == null || attacker.equals(victim) || plugin.isBypassing(attacker)) return;
        if (environmentBlocked(victim.getLocation(), ProtectionFlag.PVP, false)
                || environmentBlocked(event.getDamager().getLocation(), ProtectionFlag.PVP, false)) {
            event.setCancelled(true);
            attacker.sendMessage(plugin.message("&cEl PvP esta desactivado en esta region."));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosionDamage(EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                && cause != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) return;
        if (environmentBlocked(event.getEntity().getLocation(), ProtectionFlag.EXPLOSIONS, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (environmentBlocked(player.getLocation(), ProtectionFlag.FALL_DAMAGE, plugin.isBypassing(player))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        if (environmentBlocked(event.getLocation(), ProtectionFlag.EXPLOSIONS, false)) {
            event.setCancelled(true);
            return;
        }
        event.blockList().removeIf(block -> environmentBlocked(block.getLocation(), ProtectionFlag.EXPLOSIONS, false));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        if (environmentBlocked(event.getBlock().getLocation(), ProtectionFlag.EXPLOSIONS, false)) {
            event.setCancelled(true);
            return;
        }
        event.blockList().removeIf(block -> environmentBlocked(block.getLocation(), ProtectionFlag.EXPLOSIONS, false));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (environmentBlocked(event.getBlock().getLocation(), ProtectionFlag.FIRE, false)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (environmentBlocked(event.getBlock().getLocation(), ProtectionFlag.FIRE, false)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFireSpread(BlockSpreadEvent event) {
        Material source = event.getSource().getType();
        if (source != Material.FIRE && source != Material.SOUL_FIRE) return;
        if (environmentBlocked(event.getBlock().getLocation(), ProtectionFlag.FIRE, false)
                || environmentBlocked(event.getSource().getLocation(), ProtectionFlag.FIRE, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobGrief(EntityChangeBlockEvent event) {
        if (environmentBlocked(event.getBlock().getLocation(), ProtectionFlag.MOB_GRIEF, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (environmentBlocked(event.getLocation(), ProtectionFlag.MOB_SPAWNING, false)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (environmentBlocked(player.getLocation(), ProtectionFlag.HUNGER, plugin.isBypassing(player))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (pistonTouchesProtected(event.getBlocks(), event.getDirection())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (pistonTouchesProtected(event.getBlocks(), event.getDirection())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLiquidFlow(BlockFromToEvent event) {
        if (environmentBlocked(event.getBlock().getLocation(), ProtectionFlag.LIQUID_FLOW, false)
                || environmentBlocked(event.getToBlock().getLocation(), ProtectionFlag.LIQUID_FLOW, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.selections().clear(event.getPlayer().getUniqueId());
        plugin.clearBypass(event.getPlayer());
    }

    private boolean pistonTouchesProtected(List<Block> blocks, BlockFace direction) {
        for (Block block : blocks) {
            if (environmentBlocked(block.getLocation(), ProtectionFlag.PISTONS, false)
                    || environmentBlocked(block.getRelative(direction).getLocation(), ProtectionFlag.PISTONS, false)
                    || environmentBlocked(block.getRelative(direction.getOppositeFace()).getLocation(), ProtectionFlag.PISTONS, false)) return true;
        }
        return false;
    }

    private boolean blocked(Location location, ProtectionFlag flag, Player player, boolean membership) {
        if (plugin.isBypassing(player)) return false;
        if (plugin.worlds().isEnabled(location, flag)) return true;
        return plugin.regions().isBlocked(location, flag, player, false, membership);
    }

    private boolean environmentBlocked(Location location, ProtectionFlag flag, boolean bypass) {
        if (bypass) return false;
        return plugin.worlds().isEnabled(location, flag)
                || plugin.regions().isBlockedForEnvironment(location, flag, false);
    }

    private void deny(Player player, String action) {
        player.sendMessage(plugin.message("&cNo tienes permiso para " + action + " aqui."));
    }

    private void sendPosition(Player player, String index, Location location) {
        player.sendMessage(plugin.message("&aPosicion &f" + index + " &amarcada en &f" + location.getBlockX() + ", "
                + location.getBlockY() + ", " + location.getBlockZ() + " &7(mundo: " + location.getWorld().getName() + ")"));
    }

    private Player getPlayerDamager(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }
}
