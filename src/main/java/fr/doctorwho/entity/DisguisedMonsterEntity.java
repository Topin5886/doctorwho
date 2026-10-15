package fr.doctorwho.entity;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Monstre qui se cache sous l'apparence d'un villageois (Zygon, Slitheen).
 * <p>
 * Déguisé, il se promène paisiblement, parle comme un villageois, refuse de commercer,
 * et ni les golems de fer ni les autres monstres ne s'en prennent à lui.
 * Il se révèle quand on le frappe, ou (pour le Zygon) quand un joueur s'approche trop.
 * <p>
 * Il apparait déguisé (2 fois sur 3 dans la nature, toujours avec l'œuf ou dans un village) ;
 * avec {@code /summon}, il apparait sous sa vraie forme.
 */
public abstract class DisguisedMonsterEntity extends SimpleMonsterEntity {

    private static final TrackedData<Boolean> DISGUISED =
            DataTracker.registerData(DisguisedMonsterEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> LOOK =
            DataTracker.registerData(DisguisedMonsterEntity.class, TrackedDataHandlerRegistry.INTEGER);

    /** Nombre d'apparences de villageois différentes (métiers). */
    public static final int LOOKS = 6;

    protected DisguisedMonsterEntity(EntityType<? extends HostileEntity> type, World world, String name, int xp) {
        super(type, world, name, xp);
    }

    /** Distance à laquelle un joueur le démasque en s'approchant (0 : seulement quand on le frappe). */
    protected abstract double revealDistance();

    /** Particules et sons de la transformation. */
    protected abstract void revealEffects(ServerWorld world);

    /** Bruit qu'il fait sous sa vraie forme. */
    @Nullable
    protected SoundEvent revealedAmbientSound() {
        return null;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        dataTracker.startTracking(DISGUISED, false);
        dataTracker.startTracking(LOOK, 0);
    }

    public boolean isDisguised() {
        return dataTracker.get(DISGUISED);
    }

    /** Métier du villageois imité (0 à {@link #LOOKS} - 1). */
    public int getLook() {
        return dataTracker.get(LOOK);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason reason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        EntityData data = super.initialize(world, difficulty, reason, entityData, entityNbt);
        boolean disguise = reason != SpawnReason.COMMAND && (reason != SpawnReason.NATURAL || random.nextInt(3) != 0);
        dataTracker.set(DISGUISED, disguise);
        dataTracker.set(LOOK, random.nextInt(LOOKS));
        return data;
    }

    /** Il laisse tomber le masque. */
    public void reveal() {
        if (!isDisguised()) {
            return;
        }
        dataTracker.set(DISGUISED, false);
        if (getWorld() instanceof ServerWorld world) {
            revealEffects(world);
        }
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (isDisguised() && revealDistance() > 0 && age % 5 == 0) {
            PlayerEntity player = getWorld().getClosestPlayer(getX(), getY(), getZ(), revealDistance(),
                    EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR);
            if (player != null && canSee(player)) {
                reveal();
                setTarget(player);
            }
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!getWorld().isClient && isDisguised() && !isInvulnerableTo(source)) {
            reveal();
        }
        return super.damage(source, amount);
    }

    /** Déguisé, il ne s'en prend à personne... */
    @Override
    public boolean canTarget(LivingEntity target) {
        return !isDisguised() && super.canTarget(target);
    }

    /** ... et personne ne le prend pour cible (golems de fer compris). */
    @Override
    public boolean canTakeDamage() {
        return !isDisguised() && super.canTakeDamage();
    }

    /** Clic droit sur le faux villageois : il secoue la tête, comme un villageois sans métier. */
    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (isDisguised()) {
            playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, getSoundPitch());
            return ActionResult.success(getWorld().isClient);
        }
        return super.interactMob(player, hand);
    }

    @Override
    protected Text getDefaultName() {
        return isDisguised() ? EntityType.VILLAGER.getName() : super.getDefaultName();
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return isDisguised() ? SoundEvents.ENTITY_VILLAGER_AMBIENT : revealedAmbientSound();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return isDisguised() ? SoundEvents.ENTITY_VILLAGER_HURT : super.getHurtSound(source);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Disguised", isDisguised());
        nbt.putInt("DisguiseLook", getLook());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        dataTracker.set(DISGUISED, nbt.getBoolean("Disguised"));
        dataTracker.set(LOOK, Math.floorMod(nbt.getInt("DisguiseLook"), LOOKS));
    }
}
