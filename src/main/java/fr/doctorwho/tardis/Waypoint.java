package fr.doctorwho.tardis;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Destination enregistrée dans la console (nom, dimension, coordonnées). */
public record Waypoint(String name, RegistryKey<World> world, BlockPos pos) {

    public static final int MAX_NAME_LENGTH = 24;

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Name", name);
        nbt.putString("World", world.getValue().toString());
        nbt.putLong("Pos", pos.asLong());
        return nbt;
    }

    public static Waypoint fromNbt(NbtCompound nbt) {
        return new Waypoint(nbt.getString("Name"),
                RegistryKey.of(RegistryKeys.WORLD, new Identifier(nbt.getString("World"))),
                BlockPos.fromLong(nbt.getLong("Pos")));
    }

    public void write(PacketByteBuf buf) {
        buf.writeString(name, MAX_NAME_LENGTH);
        buf.writeIdentifier(world.getValue());
        buf.writeBlockPos(pos);
    }

    public static Waypoint read(PacketByteBuf buf) {
        return new Waypoint(buf.readString(MAX_NAME_LENGTH),
                RegistryKey.of(RegistryKeys.WORLD, buf.readIdentifier()),
                buf.readBlockPos());
    }
}
