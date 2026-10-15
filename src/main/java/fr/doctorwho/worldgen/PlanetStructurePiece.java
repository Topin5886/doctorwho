package fr.doctorwho.worldgen;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * L'unique « pièce » d'une {@link PlanetStructure} : elle couvre toute la structure
 * et la reconstruit chunk par chunk grâce à {@link StructureCanvas}.
 * Son centre et sa graine sont sauvegardés : la structure reste identique au rechargement.
 */
public class PlanetStructurePiece extends StructurePiece {

    private final PlanetStructure.Kind kind;
    private final BlockPos center;
    private final long seed;

    public PlanetStructurePiece(PlanetStructure.Kind kind, BlockPos center, long seed) {
        // La boîte part du niveau du sol : Minecraft aplanit le terrain jusqu'à cette hauteur (« beard_box »)
        super(ModStructures.PLANET_PIECE, 0, new BlockBox(
                center.getX() - kind.radius(), center.getY(), center.getZ() - kind.radius(),
                center.getX() + kind.radius(), center.getY() + kind.height(), center.getZ() + kind.radius()));
        this.kind = kind;
        this.center = center;
        this.seed = seed;
    }

    public PlanetStructurePiece(NbtCompound nbt) {
        super(ModStructures.PLANET_PIECE, nbt);
        this.kind = PlanetStructure.Kind.byId(nbt.getString("Kind"));
        this.center = new BlockPos(nbt.getInt("CX"), nbt.getInt("CY"), nbt.getInt("CZ"));
        this.seed = nbt.getLong("Seed");
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.putString("Kind", kind.asString());
        nbt.putInt("CX", center.getX());
        nbt.putInt("CY", center.getY());
        nbt.putInt("CZ", center.getZ());
        nbt.putLong("Seed", seed);
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator,
                         Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        StructureCanvas canvas = new StructureCanvas(world, chunkBox, center, seed);
        kind.build(canvas);
    }
}
