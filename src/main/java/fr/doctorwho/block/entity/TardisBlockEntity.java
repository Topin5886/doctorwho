package fr.doctorwho.block.entity;

import fr.doctorwho.registry.ModBlockEntities;
import fr.doctorwho.tardis.FlightPhase;
import fr.doctorwho.tardis.TardisFlight;
import fr.doctorwho.tardis.Variants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Extérieur du TARDIS. Garde l'identifiant qui relie cette cabine à son intérieur
 * (il suit l'objet quand on casse le TARDIS, voir la loot table) et l'état de
 * (dé)matérialisation, synchronisé vers les clients pour l'effet de clignotement.
 */
public class TardisBlockEntity extends AbstractTardisDoorBlockEntity {

    public static final String ID_KEY = "TardisId";

    @Nullable
    private UUID tardisId;
    private String variant = Variants.DEFAULT_EXTERIOR;
    private FlightPhase materialisation = FlightPhase.LANDED;
    private long materialisationStart;

    public TardisBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TARDIS, pos, state);
    }

    @Nullable
    public UUID getTardisId() {
        return tardisId;
    }

    public void setTardisId(UUID tardisId) {
        this.tardisId = tardisId;
        markDirty();
    }

    /** Apparence de la cabine (voir {@link Variants#EXTERIORS}). */
    public String getVariant() {
        return variant;
    }

    /** Côté serveur : change l'apparence et la synchronise vers les clients. */
    public void setVariant(String variant) {
        String clean = Variants.exterior(variant);
        if (clean.equals(this.variant)) {
            return;
        }
        this.variant = clean;
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
        }
    }

    public FlightPhase getMaterialisation() {
        return materialisation;
    }

    /** Côté serveur : démarre (ou termine) l'effet d'apparition / de disparition. */
    public void setMaterialisation(FlightPhase phase) {
        this.materialisation = phase;
        this.materialisationStart = world == null ? 0 : world.getTime();
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
        }
    }

    /**
     * Opacité de la cabine (1 = normale). Elle s'efface ou apparaît en « pulsant »
     * trois fois, comme dans la série.
     */
    public float getAlpha(float partialTick) {
        if (world == null || materialisation == FlightPhase.LANDED || materialisation == FlightPhase.IN_FLIGHT) {
            return 1.0f;
        }
        int duration = materialisation == FlightPhase.DEMATERIALISING ? TardisFlight.DEMAT_TICKS : TardisFlight.REMAT_TICKS;
        float t = MathHelper.clamp((world.getTime() - materialisationStart + partialTick) / duration, 0.0f, 1.0f);
        float pulse = 0.65f + 0.35f * MathHelper.cos(t * MathHelper.TAU * 3.0f);
        float base = materialisation == FlightPhase.DEMATERIALISING ? 1.0f - t : t;
        return MathHelper.clamp(base * pulse, 0.0f, 1.0f);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        tardisId = nbt.containsUuid(ID_KEY) ? nbt.getUuid(ID_KEY) : null;
        variant = Variants.exterior(nbt.getString("Variant"));
        materialisation = FlightPhase.byOrdinal(nbt.getInt("Materialisation"));
        materialisationStart = nbt.getLong("MaterialisationStart");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (tardisId != null) {
            nbt.putUuid(ID_KEY, tardisId);
        }
        nbt.putString("Variant", variant);
        nbt.putInt("Materialisation", materialisation.ordinal());
        nbt.putLong("MaterialisationStart", materialisationStart);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}
