package club.avian.factions.ftop;

/** What is actually at a position right now, read from the world. */
public record Observed(AssetKind kind, String type, int count) {
}
