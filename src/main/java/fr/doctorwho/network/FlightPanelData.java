package fr.doctorwho.network;

import fr.doctorwho.tardis.Architecture;
import fr.doctorwho.tardis.FlightPhase;
import fr.doctorwho.tardis.RoomExit;
import fr.doctorwho.tardis.TardisData;
import fr.doctorwho.tardis.TardisFlight;
import fr.doctorwho.tardis.Waypoint;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Ce que le serveur envoie au client pour afficher le panneau de la console :
 * vol, apparence (extérieur, décor, tournevis posé dans la console) et architecture.
 *
 * @param sonicVariant variante du tournevis posé dans la console, ou "" s'il n'y en a pas
 * @param exits        sorties encore libres pour construire couloirs et salles
 */
public record FlightPanelData(RegistryKey<World> currentWorld, BlockPos currentPos, FlightPhase phase,
                              RegistryKey<World> destinationWorld, BlockPos destinationPos,
                              List<RegistryKey<World>> destinations, List<Waypoint> waypoints,
                              String exteriorVariant, String desktop, String sonicVariant,
                              List<FreeExit> exits, int roomCount) {

    /** Sortie libre : son numéro (pour le serveur), la salle qui la porte et sa direction. */
    public record FreeExit(int index, String parent, Direction direction) {
    }

    public static FlightPanelData of(TardisData data, String sonicVariant) {
        Architecture.ensureExits(data);
        List<FreeExit> exits = new ArrayList<>();
        for (int i = 0; i < data.getExits().size(); i++) {
            RoomExit exit = data.getExits().get(i);
            if (!exit.used()) {
                exits.add(new FreeExit(i, exit.parent(), exit.direction()));
            }
        }
        return new FlightPanelData(data.getExteriorWorld(), data.getExteriorPos(), data.getPhase(),
                data.getDestinationWorld(), data.getDestinationPos(),
                TardisFlight.DESTINATIONS, List.copyOf(data.getWaypoints()),
                data.getExteriorVariant(), data.getDesktop(), sonicVariant, exits, data.getRoomCount());
    }

    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(currentWorld.getValue());
        buf.writeBlockPos(currentPos);
        buf.writeVarInt(phase.ordinal());
        buf.writeIdentifier(destinationWorld.getValue());
        buf.writeBlockPos(destinationPos);
        buf.writeVarInt(destinations.size());
        destinations.forEach(key -> buf.writeIdentifier(key.getValue()));
        buf.writeVarInt(waypoints.size());
        waypoints.forEach(w -> w.write(buf));
        buf.writeString(exteriorVariant);
        buf.writeString(desktop);
        buf.writeString(sonicVariant);
        buf.writeVarInt(exits.size());
        exits.forEach(e -> {
            buf.writeVarInt(e.index());
            buf.writeString(e.parent());
            buf.writeEnumConstant(e.direction());
        });
        buf.writeVarInt(roomCount);
    }

    public static FlightPanelData read(PacketByteBuf buf) {
        RegistryKey<World> currentWorld = world(buf);
        BlockPos currentPos = buf.readBlockPos();
        FlightPhase phase = FlightPhase.byOrdinal(buf.readVarInt());
        RegistryKey<World> destinationWorld = world(buf);
        BlockPos destinationPos = buf.readBlockPos();
        int count = buf.readVarInt();
        List<RegistryKey<World>> destinations = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            destinations.add(world(buf));
        }
        count = buf.readVarInt();
        List<Waypoint> waypoints = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            waypoints.add(Waypoint.read(buf));
        }
        String exterior = buf.readString();
        String desktop = buf.readString();
        String sonic = buf.readString();
        count = buf.readVarInt();
        List<FreeExit> exits = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            exits.add(new FreeExit(buf.readVarInt(), buf.readString(), buf.readEnumConstant(Direction.class)));
        }
        int roomCount = buf.readVarInt();
        return new FlightPanelData(currentWorld, currentPos, phase, destinationWorld, destinationPos, destinations,
                waypoints, exterior, desktop, sonic, exits, roomCount);
    }

    private static RegistryKey<World> world(PacketByteBuf buf) {
        return RegistryKey.of(RegistryKeys.WORLD, buf.readIdentifier());
    }
}
