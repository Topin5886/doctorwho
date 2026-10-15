package fr.doctorwho.block.entity;

import fr.doctorwho.block.TardisConsoleBlock;
import fr.doctorwho.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Console de la salle de commande : animations (rotor temporel) et logement
 * du tournevis sonique, synchronisé vers les clients pour l'afficher.
 */
public class TardisConsoleBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.tardis_console.idle");
    private static final RawAnimation FLIGHT = RawAnimation.begin().thenLoop("animation.tardis_console.flight");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ItemStack sonic = ItemStack.EMPTY;

    public TardisConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TARDIS_CONSOLE, pos, state);
    }

    public ItemStack getSonic() {
        return sonic;
    }

    public void setSonic(ItemStack sonic) {
        this.sonic = sonic;
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "rotor", 10, state ->
                state.setAndContinue(getCachedState().get(TardisConsoleBlock.FLYING) ? FLIGHT : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        sonic = nbt.contains("Sonic") ? ItemStack.fromNbt(nbt.getCompound("Sonic")) : ItemStack.EMPTY;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (!sonic.isEmpty()) {
            nbt.put("Sonic", sonic.writeNbt(new NbtCompound()));
        }
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
