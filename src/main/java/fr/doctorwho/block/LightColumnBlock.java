package fr.doctorwho.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Colonne de lumière : huit tubes lumineux autour d'un noyau, qui s'empilent en grande colonne.
 * Animée par sa texture (vagues de lumière qui montent) : aucun calcul par bloc, même pour une
 * colonne de dix blocs. Couleur choisie par la propriété « color » (orange, cyan, bleu).
 */
public class LightColumnBlock extends Block {

    public enum Color implements StringIdentifiable {
        ORANGE("orange"), CYAN("cyan"), BLUE("blue");

        private final String id;

        Color(String id) {
            this.id = id;
        }

        @Override
        public String asString() {
            return id;
        }
    }

    public static final EnumProperty<Color> COLOR = EnumProperty.of("color", Color.class);
    private static final VoxelShape SHAPE = Block.createCuboidShape(2, 0, 2, 14, 16, 14);

    public LightColumnBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(COLOR, Color.ORANGE));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
}
