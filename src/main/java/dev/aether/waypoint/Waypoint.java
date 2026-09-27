package dev.aether.waypoint;

/**
 * One saved location: a name, a world and a position, plus the colour the marker draws with.
 * <p>
 * A value object on purpose: waypoints are created, renamed, moved and deleted from the UI and the
 * chat command, and every mutation goes through {@link WaypointManager} so the config write happens
 * in exactly one place. {@code world} is the dimension id string ("minecraft:overworld",
 * "minecraft:the_nether", ...), which is what the renderer compares against the player's current
 * dimension - a waypoint in another dimension is stored but not drawn.
 */
public final class Waypoint {
    private final String name;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final int color;
    private final boolean enabled;

    public Waypoint(String name, String world, int x, int y, int z, int color, boolean enabled) {
        this.name = name == null ? "" : name;
        this.world = world == null ? "" : world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        this.enabled = enabled;
    }

    public String name() {
        return this.name;
    }

    /** The dimension id, e.g. {@code minecraft:overworld}. */
    public String world() {
        return this.world;
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    public int z() {
        return this.z;
    }

    public int color() {
        return this.color;
    }

    public boolean enabled() {
        return this.enabled;
    }

    /** @return a copy with {@code enabled} flipped; the manager uses this for the toggle. */
    public Waypoint withEnabled(boolean value) {
        return new Waypoint(this.name, this.world, this.x, this.y, this.z, this.color, value);
    }

    /** @return a copy moved to a new position. */
    public Waypoint withPosition(int x, int y, int z) {
        return new Waypoint(this.name, this.world, x, y, z, this.color, this.enabled);
    }

    /** @return a copy with a new name. */
    public Waypoint withName(String value) {
        return new Waypoint(value, this.world, this.x, this.y, this.z, this.color, this.enabled);
    }

    /** @return a copy with a new marker colour. */
    public Waypoint withColor(int value) {
        return new Waypoint(this.name, this.world, this.x, this.y, this.z, value, this.enabled);
    }

    /** @return a copy that belongs to another dimension. */
    public Waypoint withWorld(String value) {
        return new Waypoint(this.name, value, this.x, this.y, this.z, this.color, this.enabled);
    }

    @Override
    public String toString() {
        return this.name + " (" + this.world + " " + this.x + " " + this.y + " " + this.z + ")";
    }
}
