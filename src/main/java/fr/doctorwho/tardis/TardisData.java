package fr.doctorwho.tardis;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ce que le monde retient d'un TARDIS : son emplacement intérieur (index de zone),
 * la position de son extérieur, son vol en cours et ses waypoints.
 */
public class TardisData {

    public static final int MAX_WAYPOINTS = 6;

    private final UUID id;
    private final int index;
    private RegistryKey<World> exteriorWorld = World.OVERWORLD;
    private BlockPos exteriorPos = BlockPos.ORIGIN;
    private Direction exteriorFacing = Direction.NORTH;
    private boolean interiorGenerated;
    private int interiorVersion;
    private String exteriorVariant = Variants.DEFAULT_EXTERIOR;
    private String desktop = Variants.DEFAULT_DESKTOP;

    private FlightPhase phase = FlightPhase.LANDED;
    private int phaseTicks;
    private RegistryKey<World> destinationWorld = World.OVERWORLD;
    private BlockPos destinationPos = BlockPos.ORIGIN;

    private final List<Waypoint> waypoints = new ArrayList<>();

    /** Extension de l'intérieur : sorties disponibles / utilisées et volumes déjà construits. */
    private final List<RoomExit> exits = new ArrayList<>();
    private final List<BlockBox> occupied = new ArrayList<>();
    private int roomCount;

    public TardisData(UUID id, int index) {
        this.id = id;
        this.index = index;
    }

    public UUID getId() {
        return id;
    }

    public int getIndex() {
        return index;
    }

    // --- Extérieur ---------------------------------------------------------------------

    public RegistryKey<World> getExteriorWorld() {
        return exteriorWorld;
    }

    public BlockPos getExteriorPos() {
        return exteriorPos;
    }

    public Direction getExteriorFacing() {
        return exteriorFacing;
    }

    public void setExterior(RegistryKey<World> world, BlockPos pos, Direction facing) {
        this.exteriorWorld = world;
        this.exteriorPos = pos.toImmutable();
        this.exteriorFacing = facing;
    }

    // --- Intérieur ---------------------------------------------------------------------

    public boolean isInteriorGenerated() {
        return interiorGenerated;
    }

    public void setInteriorGenerated(boolean interiorGenerated) {
        this.interiorGenerated = interiorGenerated;
    }

    public int getInteriorVersion() {
        return interiorVersion;
    }

    public void setInteriorVersion(int interiorVersion) {
        this.interiorVersion = interiorVersion;
    }

    // --- Apparence ---------------------------------------------------------------------

    public String getExteriorVariant() {
        return exteriorVariant;
    }

    public void setExteriorVariant(String exteriorVariant) {
        this.exteriorVariant = Variants.exterior(exteriorVariant);
    }

    public String getDesktop() {
        return desktop;
    }

    public void setDesktop(String desktop) {
        this.desktop = Variants.desktop(desktop);
    }

    // --- Vol ---------------------------------------------------------------------------

    public FlightPhase getPhase() {
        return phase;
    }

    public boolean isLanded() {
        return phase == FlightPhase.LANDED;
    }

    public void setPhase(FlightPhase phase) {
        this.phase = phase;
        this.phaseTicks = 0;
    }

    /** Avance d'un tick dans l'étape en cours et renvoie le nombre de ticks écoulés. */
    public int tickPhase() {
        return ++phaseTicks;
    }

    public RegistryKey<World> getDestinationWorld() {
        return destinationWorld;
    }

    public BlockPos getDestinationPos() {
        return destinationPos;
    }

    public void setDestination(RegistryKey<World> world, BlockPos pos) {
        this.destinationWorld = world;
        this.destinationPos = pos.toImmutable();
    }

    // --- Waypoints ---------------------------------------------------------------------

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public boolean addWaypoint(Waypoint waypoint) {
        if (waypoints.size() >= MAX_WAYPOINTS) {
            return false;
        }
        waypoints.add(waypoint);
        return true;
    }

    public void removeWaypoint(int i) {
        if (i >= 0 && i < waypoints.size()) {
            waypoints.remove(i);
        }
    }

    // --- Architecture (couloirs et salles) ----------------------------------------------

    public List<RoomExit> getExits() {
        return exits;
    }

    public List<BlockBox> getOccupied() {
        return occupied;
    }

    public int getRoomCount() {
        return roomCount;
    }

    public void incrementRoomCount() {
        roomCount++;
    }

    // --- Sauvegarde --------------------------------------------------------------------

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("Id", id);
        nbt.putInt("Index", index);
        nbt.putString("ExteriorWorld", exteriorWorld.getValue().toString());
        nbt.putLong("ExteriorPos", exteriorPos.asLong());
        nbt.putString("ExteriorFacing", exteriorFacing.asString());
        nbt.putBoolean("InteriorGenerated", interiorGenerated);
        nbt.putInt("InteriorVersion", interiorVersion);
        nbt.putString("ExteriorVariant", exteriorVariant);
        nbt.putString("Desktop", desktop);
        nbt.putInt("Phase", phase.ordinal());
        nbt.putInt("PhaseTicks", phaseTicks);
        nbt.putString("DestinationWorld", destinationWorld.getValue().toString());
        nbt.putLong("DestinationPos", destinationPos.asLong());
        NbtList list = new NbtList();
        waypoints.forEach(w -> list.add(w.toNbt()));
        nbt.put("Waypoints", list);
        NbtList exitList = new NbtList();
        exits.forEach(e -> exitList.add(e.toNbt()));
        nbt.put("Exits", exitList);
        NbtList boxList = new NbtList();
        occupied.forEach(b -> boxList.add(new NbtIntArray(new int[]{b.getMinX(), b.getMinY(), b.getMinZ(),
                b.getMaxX(), b.getMaxY(), b.getMaxZ()})));
        nbt.put("Occupied", boxList);
        nbt.putInt("RoomCount", roomCount);
        return nbt;
    }

    public static TardisData fromNbt(NbtCompound nbt) {
        TardisData data = new TardisData(nbt.getUuid("Id"), nbt.getInt("Index"));
        Direction facing = Direction.byName(nbt.getString("ExteriorFacing"));
        data.setExterior(worldKey(nbt.getString("ExteriorWorld")),
                BlockPos.fromLong(nbt.getLong("ExteriorPos")),
                facing == null ? Direction.NORTH : facing);
        data.interiorGenerated = nbt.getBoolean("InteriorGenerated");
        data.interiorVersion = nbt.getInt("InteriorVersion");
        data.setExteriorVariant(nbt.getString("ExteriorVariant"));
        data.setDesktop(nbt.getString("Desktop"));
        data.phase = FlightPhase.byOrdinal(nbt.getInt("Phase"));
        data.phaseTicks = nbt.getInt("PhaseTicks");
        if (nbt.contains("DestinationWorld")) {
            data.setDestination(worldKey(nbt.getString("DestinationWorld")), BlockPos.fromLong(nbt.getLong("DestinationPos")));
        }
        NbtList list = nbt.getList("Waypoints", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            data.waypoints.add(Waypoint.fromNbt(list.getCompound(i)));
        }
        NbtList exitList = nbt.getList("Exits", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < exitList.size(); i++) {
            data.exits.add(RoomExit.fromNbt(exitList.getCompound(i)));
        }
        NbtList boxList = nbt.getList("Occupied", NbtElement.INT_ARRAY_TYPE);
        for (int i = 0; i < boxList.size(); i++) {
            int[] b = boxList.getIntArray(i);
            if (b.length == 6) {
                data.occupied.add(new BlockBox(b[0], b[1], b[2], b[3], b[4], b[5]));
            }
        }
        data.roomCount = nbt.getInt("RoomCount");
        return data;
    }

    private static RegistryKey<World> worldKey(String id) {
        return RegistryKey.of(RegistryKeys.WORLD, new Identifier(id));
    }
}
