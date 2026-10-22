package Unbound.unboundProtection.model;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ProtectedRegion {
    private final String id;
    private final String worldName;
    private final int minX, minY, minZ, maxX, maxY, maxZ;
    private final UUID ownerId;
    private final String ownerName;
    private final Set<UUID> members = new HashSet<>();
    private final Map<ProtectionFlag, Boolean> flags = new EnumMap<>(ProtectionFlag.class);

    public ProtectedRegion(String id, String worldName, int x1, int y1, int z1, int x2, int y2, int z2,
                           UUID ownerId, String ownerName) {
        this.id = id.toLowerCase(Locale.ROOT);
        this.worldName = worldName;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
        this.ownerId = ownerId;
        this.ownerName = ownerName == null ? "desconocido" : ownerName;
        for (ProtectionFlag flag : ProtectionFlag.values()) flags.put(flag, flag.enabledByDefault());
    }

    public String id() { return id; }
    public String worldName() { return worldName; }
    public int minX() { return minX; }
    public int minY() { return minY; }
    public int minZ() { return minZ; }
    public int maxX() { return maxX; }
    public int maxY() { return maxY; }
    public int maxZ() { return maxZ; }
    public UUID ownerId() { return ownerId; }
    public String ownerName() { return ownerName; }
    public Set<UUID> members() { return members; }
    public Map<ProtectionFlag, Boolean> flags() { return flags; }
    public boolean flag(ProtectionFlag flag) { return flags.getOrDefault(flag, flag.enabledByDefault()); }
    public void setFlag(ProtectionFlag flag, boolean value) { flags.put(flag, value); }

    public boolean contains(Location location) {
        World world = location.getWorld();
        if (world == null || !world.getName().equalsIgnoreCase(worldName)) return false;
        int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public boolean isOwnerOrMember(UUID playerId) {
        return playerId != null && (playerId.equals(ownerId) || members.contains(playerId));
    }

    public long volume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }
}
