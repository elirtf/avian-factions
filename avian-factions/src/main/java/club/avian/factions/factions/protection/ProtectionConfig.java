package club.avian.factions.factions.protection;

import club.avian.factions.api.config.ConfigErrors;
import club.avian.factions.api.faction.Interaction;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The {@code protection} section of {@code factions.conf} (spec §9).
 *
 * <p>Two separate things, kept separate deliberately — the split every surveyed plugin ended up
 * with: <em>permissions</em> are what a non-member may do in a claim, <em>flags</em> are world
 * behaviour inside a claim that has nothing to do with who is standing there.
 */
@ConfigSerializable
public final class ProtectionConfig {

    @Comment("""
            What a player who is NOT a member of the owning faction may still do in its claims.
            Anything not listed is denied. Valid: BUILD, CONTAINER, DOOR, SWITCH, ENTITY,
            ITEM_USE, DAMAGE_ENTITY. Default denies everything, which is spec §9's default set.""")
    private List<String> nonMemberAllowed = List.of();

    @Comment("Permission node that bypasses every claim check. Staff only.")
    private String bypassPermission = "avian.factions.bypass";

    @Comment("Block damage from explosions inside claims. false protects claims from TNT and\n"
            + "creepers entirely; raiding turns this on for raidable factions regardless.")
    private boolean explosionsInClaims = false;

    @Comment("Fire spreading and burning blocks inside claims.")
    private boolean fireSpreadInClaims = false;

    @Comment("Fluids flowing from outside a claim into it. Stops cobble-generator griefing.")
    private boolean fluidFlowIntoClaims = false;

    @Comment("Seconds between repeats of the same denial message to the same player. Interact and\n"
            + "hopper events fire every tick, so unthrottled messages would flood chat.")
    private int denyMessageCooldownSeconds = 3;

    private transient Set<Interaction> parsed;

    /** The non-member permission set, parsed once. */
    public Set<Interaction> nonMemberAllowed() {
        if (parsed == null) {
            var set = EnumSet.noneOf(Interaction.class);
            for (var name : nonMemberAllowed) {
                set.add(Interaction.valueOf(name.toUpperCase(java.util.Locale.ROOT)));
            }
            parsed = set;
        }
        return parsed;
    }

    public String bypassPermission() {
        return bypassPermission;
    }

    public boolean explosionsInClaims() {
        return explosionsInClaims;
    }

    public boolean fireSpreadInClaims() {
        return fireSpreadInClaims;
    }

    public boolean fluidFlowIntoClaims() {
        return fluidFlowIntoClaims;
    }

    public int denyMessageCooldownSeconds() {
        return denyMessageCooldownSeconds;
    }

    public void validate(ConfigErrors e) {
        for (var name : nonMemberAllowed) {
            try {
                Interaction.valueOf(name.toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                e.add("protection.non-member-allowed", "'" + name + "' is not an interaction kind; valid: "
                        + java.util.Arrays.toString(Interaction.values()));
            }
        }
        e.check(!bypassPermission.isBlank(), "protection.bypass-permission", "must not be blank");
        e.check(denyMessageCooldownSeconds >= 0, "protection.deny-message-cooldown-seconds", "must be >= 0");
    }
}
