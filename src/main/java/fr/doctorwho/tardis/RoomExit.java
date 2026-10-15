package fr.doctorwho.tardis;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Sortie dans un mur de l'intérieur, où l'on peut faire pousser un couloir et une salle.
 *
 * @param anchor position (au niveau des pieds) du bloc de mur à percer
 * @param direction direction vers l'extérieur de la salle
 * @param parent type de la salle qui porte cette sortie ("console", "bedroom"...)
 * @param used vrai si un couloir part déjà d'ici
 */
public record RoomExit(BlockPos anchor, Direction direction, String parent, boolean used) {

    public RoomExit asUsed() {
        return new RoomExit(anchor, direction, parent, true);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putLong("Anchor", anchor.asLong());
        nbt.putString("Direction", direction.asString());
        nbt.putString("Parent", parent);
        nbt.putBoolean("Used", used);
        return nbt;
    }

    public static RoomExit fromNbt(NbtCompound nbt) {
        Direction direction = Direction.byName(nbt.getString("Direction"));
        return new RoomExit(BlockPos.fromLong(nbt.getLong("Anchor")),
                direction == null ? Direction.NORTH : direction,
                nbt.getString("Parent"), nbt.getBoolean("Used"));
    }
}
