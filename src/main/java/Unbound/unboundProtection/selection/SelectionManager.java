package Unbound.unboundProtection.selection;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SelectionManager {
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();

    public void setPos1(UUID player, Location location) { pos1.put(player, location.clone()); }
    public void setPos2(UUID player, Location location) { pos2.put(player, location.clone()); }
    public Location getPos1(UUID player) { return pos1.get(player); }
    public Location getPos2(UUID player) { return pos2.get(player); }
    public boolean hasCompleteSelection(UUID player) { return pos1.containsKey(player) && pos2.containsKey(player); }
    public void clear(UUID player) { pos1.remove(player); pos2.remove(player); }
}
