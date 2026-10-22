package Unbound.unboundProtection.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
//protecciones actuales
public enum ProtectionFlag {
    BLOCK_BREAK("block-break", true, true, "Bloquea romper bloques"),
    BLOCK_PLACE("block-place", true, true, "Bloquea colocar bloques"),
    PVP("pvp", false, false, "Bloquea el daño entre jugadores"),
    EXPLOSIONS("explosions", true, false, "Impide que explosiones destruyaln boques"),
    FIRE("fire", true, false, "Bloquea fuego, ignicion y propagacion"),
    MOB_GRIEF("mob-grief", true, false, "Impide que mobs cambien o destruyan bloques"),
    INTERACT("interact", true, true, "Bloquea puertas, botones y otras interacciones"),
    CONTAINER("container", true, true, "Bloquea abrir cofres y contenedores"),
    BUCKET("bucket", true, true, "Bloquea usar cubos"),
    ITEM_FRAME("item-frame", true, true, "Protege marcos, marcos luminosos y soportes de armadura"),
    FALL_DAMAGE("fall-damage", false, false, "dano de caida"),
    MOB_SPAWNING("mob-spawning", false, false, "Impide aparecer entidades no jugador"),
    HUNGER("hunger", false, false, "Impide perder hambre"),
    PISTONS("pistons", true, false, "Impide que pistones muevan bloques dentro de la region"),
    LIQUID_FLOW("liquid-flow", true, false, "Impide que fluidos entren o salgan de la region");

    private final String key;
    private final boolean enabledByDefault;
    private final boolean membersCanBuild;
    private final String description;

    ProtectionFlag(String key, boolean enabledByDefault, boolean membersCanBuild, String description) {
        this.key = key;
        this.enabledByDefault = enabledByDefault;
        this.membersCanBuild = membersCanBuild;
        this.description = description;
    }

    public String key() { return key; }
    public boolean enabledByDefault() { return enabledByDefault; }
    public boolean membersCanBuild() { return membersCanBuild; }
    public String description() { return description; }

    public static Optional<ProtectionFlag> fromKey(String key) {
        if (key == null) return Optional.empty();
        String normalized = key.toLowerCase(Locale.ROOT).replace('_', '-');
        return Arrays.stream(values()).filter(flag -> flag.key.equals(normalized)).findFirst();
    }
}
