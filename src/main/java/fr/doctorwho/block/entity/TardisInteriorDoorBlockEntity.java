package fr.doctorwho.block.entity;

import fr.doctorwho.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

/** Porte intérieure : seulement les animations (le TARDIS se retrouve par la position). */
public class TardisInteriorDoorBlockEntity extends AbstractTardisDoorBlockEntity {

    public TardisInteriorDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TARDIS_INTERIOR_DOOR, pos, state);
    }
}
