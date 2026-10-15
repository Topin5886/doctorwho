package fr.doctorwho.worldgen;

import fr.doctorwho.DoctorWhoMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.world.gen.structure.StructureType;

/**
 * Structures des planètes. Le code Java fournit le type « planet_structure » ;
 * les structures elles-mêmes sont déclarées en JSON dans {@code data/doctorwho/worldgen} :
 * <ul>
 *     <li>{@code structure/*.json} : quel bâtiment, sur quelle planète (tag de biome) ;</li>
 *     <li>{@code structure_set/*.json} : espacement entre deux exemplaires (en chunks).</li>
 * </ul>
 */
public final class ModStructures {

    public static final StructureType<PlanetStructure> PLANET_STRUCTURE = Registry.register(
            Registries.STRUCTURE_TYPE, DoctorWhoMod.id("planet_structure"), () -> PlanetStructure.CODEC);

    public static final StructurePieceType PLANET_PIECE = Registry.register(
            Registries.STRUCTURE_PIECE, DoctorWhoMod.id("planet_piece"),
            (StructurePieceType.Simple) PlanetStructurePiece::new);

    private ModStructures() {
    }

    public static void register() {
        // Charge la classe : les types sont enregistrés par les champs statiques
    }
}
