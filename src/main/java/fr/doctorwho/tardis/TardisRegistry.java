package fr.doctorwho.tardis;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Liste de tous les TARDIS du monde, sauvegardée dans data/doctorwho_tardis.dat
 * (dans le dossier de la sauvegarde, côté Overworld).
 */
public class TardisRegistry extends PersistentState {

    private static final String NAME = "doctorwho_tardis";

    private final Map<UUID, TardisData> byId = new HashMap<>();
    private final Map<Integer, TardisData> byIndex = new HashMap<>();
    private int nextIndex;

    public static TardisRegistry get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(TardisRegistry::fromNbt, TardisRegistry::new, NAME);
    }

    @Nullable
    public TardisData get(UUID id) {
        return byId.get(id);
    }

    @Nullable
    public TardisData getByIndex(int index) {
        return byIndex.get(index);
    }

    public Collection<TardisData> all() {
        return byId.values();
    }

    /** Nouveau TARDIS : on lui réserve la prochaine zone libre de la dimension intérieure. */
    public TardisData create() {
        TardisData data = new TardisData(UUID.randomUUID(), nextIndex++);
        add(data);
        markDirty();
        return data;
    }

    private void add(TardisData data) {
        byId.put(data.getId(), data);
        byIndex.put(data.getIndex(), data);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        byId.values().forEach(data -> list.add(data.toNbt()));
        nbt.put("Tardis", list);
        nbt.putInt("NextIndex", nextIndex);
        return nbt;
    }

    private static TardisRegistry fromNbt(NbtCompound nbt) {
        TardisRegistry registry = new TardisRegistry();
        NbtList list = nbt.getList("Tardis", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            registry.add(TardisData.fromNbt(list.getCompound(i)));
        }
        registry.nextIndex = nbt.getInt("NextIndex");
        return registry;
    }
}
