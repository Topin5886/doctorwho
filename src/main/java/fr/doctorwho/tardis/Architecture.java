package fr.doctorwho.tardis;

import fr.doctorwho.block.FacingDecorBlock;
import fr.doctorwho.block.TardisConsoleBlock;
import fr.doctorwho.registry.ModBlocks;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Extension de l'intérieur du TARDIS : depuis une sortie (dans un mur), on creuse
 * un couloir puis on construit une salle meublée au bout. Chaque salle ouvre à son
 * tour une sortie vers l'avant (trois pour un croisement), ce qui permet de
 * construire tout un réseau de couloirs.
 * <p>
 * Repère local d'une salle : « a » vers l'avant (sens du couloir), « b » vers la
 * droite, « dy » vers le haut depuis le niveau des pieds.
 */
public final class Architecture {

    /** Types de salles disponibles (clés de traduction room.doctorwho.&lt;type&gt;). */
    public static final List<String> ROOM_TYPES = List.of(
            "bedroom", "kitchen", "library", "console", "pool", "garden", "workshop", "junction");

    public static final int MAX_ROOMS = 16;

    private static final int CORRIDOR_LENGTH = 8;
    /** Demi-côté des salles : murs à ±5 du centre, intérieur de 9 x 9. */
    private static final int HALF = 5;
    /** Plafond des salles (au-dessus du niveau des pieds). */
    private static final int ROOM_TOP = 5;
    /** Rayon de la salle de console, à éviter. */
    private static final int CONSOLE_ROOM = 12;
    /** Limite de la zone de ce TARDIS dans la dimension intérieure. */
    private static final int ZONE_LIMIT = TardisManager.SPACING / 2 - 16;

    private static final BlockState AIR = Blocks.AIR.getDefaultState();

    private Architecture() {
    }

    /** Les trois sorties de la salle de console (nord, est, ouest ; la porte est au sud). */
    public static void ensureExits(TardisData data) {
        if (!data.getExits().isEmpty()) {
            return;
        }
        BlockPos center = TardisManager.interiorCenter(data);
        for (Direction direction : new Direction[]{Direction.NORTH, Direction.EAST, Direction.WEST}) {
            data.getExits().add(new RoomExit(center.add(direction.getOffsetX() * 10, 1, direction.getOffsetZ() * 10),
                    direction, "console", false));
        }
    }

    /** Construit couloir + salle à partir d'une sortie. Renvoie un message d'erreur, ou null si c'est fait. */
    @Nullable
    public static Text build(ServerWorld world, TardisData data, int exitIndex, String type) {
        ensureExits(data);
        if (exitIndex < 0 || exitIndex >= data.getExits().size() || !ROOM_TYPES.contains(type)) {
            return Text.translatable("message.doctorwho.architecture.invalid");
        }
        RoomExit exit = data.getExits().get(exitIndex);
        if (exit.used()) {
            return Text.translatable("message.doctorwho.architecture.used");
        }
        if (data.getRoomCount() >= MAX_ROOMS) {
            return Text.translatable("message.doctorwho.architecture.full", MAX_ROOMS);
        }

        Direction forward = exit.direction();
        Direction right = forward.rotateYClockwise();
        BlockPos start = exit.anchor();
        BlockPos roomCenter = start.offset(forward, CORRIDOR_LENGTH + 1 + HALF);

        BlockBox corridorBox = box(start.offset(forward), start.offset(forward, CORRIDOR_LENGTH), right, 2, -1, 3);
        BlockBox roomBox = box(roomCenter.offset(forward, -HALF), roomCenter.offset(forward, HALF), right, HALF, -3, ROOM_TOP);
        // Le couloir part du mur de la salle de console : seule la salle doit l'éviter
        if (!fits(data, corridorBox, false) || !fits(data, roomBox, true)) {
            return Text.translatable("message.doctorwho.architecture.no_space");
        }

        InteriorBuilder.Theme theme = InteriorBuilder.theme(data.getDesktop());
        buildCorridor(world, exit, theme);
        buildRoom(world, roomCenter, forward, theme, type, ConsoleStyle.forDesktop(data.getDesktop()));

        data.getExits().set(exitIndex, exit.asUsed());
        data.getOccupied().add(corridorBox);
        data.getOccupied().add(roomBox);
        data.incrementRoomCount();
        // Nouvelles sorties : vers l'avant, et sur les côtés pour un croisement
        data.getExits().add(new RoomExit(roomCenter.offset(forward, HALF), forward, type, false));
        if (type.equals("junction")) {
            data.getExits().add(new RoomExit(roomCenter.offset(right, HALF), right, type, false));
            data.getExits().add(new RoomExit(roomCenter.offset(right, -HALF), right.getOpposite(), type, false));
        }
        return null;
    }

    /**
     * Après une reconstruction de la salle de console (changement de décor), on rouvre
     * les passages vers les couloirs déjà construits.
     */
    public static void reopenConsoleExits(ServerWorld world, TardisData data) {
        InteriorBuilder.Theme theme = InteriorBuilder.theme(data.getDesktop());
        for (RoomExit exit : data.getExits()) {
            if (exit.used() && exit.parent().equals("console")) {
                buildCorridor(world, exit, theme);
            }
        }
    }

    // --- Construction --------------------------------------------------------------------

    private static void buildCorridor(ServerWorld world, RoomExit exit, InteriorBuilder.Theme theme) {
        Direction forward = exit.direction();
        Direction right = forward.rotateYClockwise();
        BlockPos start = exit.anchor();
        BlockState wall = theme.wall().getDefaultState();
        BlockState wallLit = theme.wallLit().getDefaultState();
        BlockState floor = theme.platform().getDefaultState();
        BlockState ceiling = theme.ceiling().getDefaultState();
        BlockState light = ModBlocks.HEX_LIGHT.getDefaultState();
        for (int a = 1; a <= CORRIDOR_LENGTH; a++) {
            for (int b = -2; b <= 2; b++) {
                for (int dy = -1; dy <= 3; dy++) {
                    BlockState state = AIR;
                    if (dy == -1) {
                        state = floor;
                    } else if (dy == 3) {
                        state = b == 0 && a % 3 == 1 ? light : ceiling;
                    } else if (Math.abs(b) == 2) {
                        state = dy == 1 && a % 3 == 0 ? wallLit : wall;
                    }
                    set(world, local(start, forward, right, a, b, dy), state);
                }
            }
        }
        carveOpening(world, start, forward, right, floor);
    }

    /** Ouvre un passage de 3 x 3 dans un mur, avec son sol. */
    private static void carveOpening(ServerWorld world, BlockPos pos, Direction forward, Direction right, BlockState floor) {
        for (int b = -1; b <= 1; b++) {
            set(world, local(pos, forward, right, 0, b, -1), floor);
            for (int dy = 0; dy <= 2; dy++) {
                set(world, local(pos, forward, right, 0, b, dy), AIR);
            }
        }
    }

    private static void buildRoom(ServerWorld world, BlockPos center, Direction forward, InteriorBuilder.Theme theme, String type,
                                  ConsoleStyle style) {
        Direction right = forward.rotateYClockwise();
        BlockState wall = theme.wall().getDefaultState();
        BlockState wallLit = theme.wallLit().getDefaultState();
        BlockState base = theme.base().getDefaultState();
        BlockState floor = theme.platform().getDefaultState();
        BlockState ceiling = theme.ceiling().getDefaultState();
        BlockState light = ModBlocks.HEX_LIGHT.getDefaultState();

        for (int a = -HALF; a <= HALF; a++) {
            for (int b = -HALF; b <= HALF; b++) {
                for (int dy = -1; dy <= ROOM_TOP; dy++) {
                    boolean side = Math.abs(a) == HALF || Math.abs(b) == HALF;
                    BlockState state = AIR;
                    if (dy == -1) {
                        state = side ? base : floor;
                    } else if (dy == ROOM_TOP) {
                        state = Math.floorMod(a, 3) == 0 && Math.floorMod(b, 3) == 0 && !side ? light : ceiling;
                    } else if (side) {
                        boolean lit = dy == 2 && (Math.abs(a) + Math.abs(b)) % 3 == 0;
                        state = dy == 0 ? base : lit ? wallLit : wall;
                    }
                    set(world, local(center, forward, right, a, b, dy), state);
                }
            }
        }
        carveOpening(world, local(center, forward, right, -HALF, 0, 0), forward, right, floor);
        Furnisher f = new Furnisher(world, center, forward, right);
        switch (type) {
            case "bedroom" -> f.bedroom();
            case "kitchen" -> f.kitchen();
            case "library" -> f.library();
            case "console" -> f.console(style);
            case "pool" -> f.pool();
            case "garden" -> f.garden();
            case "workshop" -> f.workshop();
            default -> f.junction();
        }
    }

    /** Pose les meubles d'une salle dans son repère local. */
    private record Furnisher(ServerWorld world, BlockPos center, Direction forward, Direction right) {

        void put(int a, int b, int dy, BlockState state) {
            set(world, local(center, forward, right, a, b, dy), state);
        }

        void put(int a, int b, int dy, Block block) {
            put(a, b, dy, block.getDefaultState());
        }

        /** Applique murale au mur de gauche ou de droite, tournée vers le centre. */
        void lamp(int a, int side) {
            Direction facing = side > 0 ? right.getOpposite() : right;
            put(a, side * (HALF - 1), 2, ModBlocks.WALL_LAMP.getDefaultState().with(FacingDecorBlock.FACING, facing));
        }

        void bedroom() {
            put(4, 0, 0, Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, forward).with(BedBlock.PART, BedPart.HEAD));
            put(3, 0, 0, Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, forward).with(BedBlock.PART, BedPart.FOOT));
            for (int side : new int[]{-2, 2}) {
                put(4, side, 0, Blocks.BARREL);
                put(4, side, 1, Blocks.LANTERN);
            }
            for (int a = -1; a <= 2; a++) {
                for (int b = -2; b <= 2; b++) {
                    put(a, b, 0, Blocks.RED_CARPET);
                }
            }
            for (int a = 0; a <= 1; a++) {
                put(a, -4, 0, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, right));
                put(a, 4, 0, Blocks.BOOKSHELF);
                put(a, 4, 1, Blocks.BOOKSHELF);
            }
            lamp(-2, -1);
            lamp(-2, 1);
        }

        void kitchen() {
            Direction toCenter = forward.getOpposite();
            Block[] counter = {Blocks.BARREL, Blocks.SMOKER, Blocks.FURNACE, Blocks.CRAFTING_TABLE, null,
                    Blocks.BARREL, Blocks.SMOKER, Blocks.CHEST, Blocks.BARREL};
            for (int i = 0; i < counter.length; i++) {
                int b = i - 4;
                if (counter[i] == null) {
                    put(4, b, 0, Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 3));
                } else if (counter[i] == Blocks.FURNACE || counter[i] == Blocks.SMOKER) {
                    put(4, b, 0, counter[i].getDefaultState().with(FurnaceBlock.FACING, toCenter));
                } else if (counter[i] == Blocks.CHEST) {
                    put(4, b, 0, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, toCenter));
                } else {
                    put(4, b, 0, counter[i]);
                }
            }
            // « Frigo » et table avec ses chaises
            put(2, -4, 0, Blocks.IRON_BLOCK);
            put(2, -4, 1, Blocks.IRON_BLOCK);
            for (int b = -1; b <= 1; b++) {
                put(0, b, 0, Blocks.OAK_FENCE);
                put(0, b, 1, Blocks.OAK_PRESSURE_PLATE);
                put(-1, b, 0, Blocks.OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, forward.getOpposite()));
                put(1, b, 0, Blocks.OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, forward));
            }
            lamp(-2, -1);
            lamp(-2, 1);
        }

        void library() {
            for (int a = -3; a <= 4; a++) {
                for (int dy = 0; dy <= 2; dy++) {
                    put(a, -4, dy, Blocks.BOOKSHELF);
                    put(a, 4, dy, Blocks.BOOKSHELF);
                }
            }
            for (int b = -3; b <= 3; b++) {
                for (int dy = 0; dy <= 2; dy++) {
                    put(4, b, dy, Blocks.BOOKSHELF);
                }
            }
            put(1, 0, 0, Blocks.LECTERN.getDefaultState().with(LecternBlock.FACING, forward.getOpposite()));
            put(3, 0, 0, Blocks.ENCHANTING_TABLE);
            for (int a = -2; a <= 2; a++) {
                for (int b = -2; b <= 2; b++) {
                    put(a, b, 0, a == 1 && b == 0 ? Blocks.LECTERN.getDefaultState()
                            .with(LecternBlock.FACING, forward.getOpposite()) : Blocks.BROWN_CARPET.getDefaultState());
                }
            }
            put(-2, -2, 0, Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, right.getOpposite()));
            put(-2, 2, 0, Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, right));
            put(-1, 0, 0, Blocks.LANTERN);
        }

        /** Salle de console secondaire : on peut piloter le TARDIS d'ici aussi. */
        void console(ConsoleStyle style) {
            TardisConsoleBlock.place(world, local(center, forward, right, 0, 0, 0), style);
            for (int a : new int[]{-3, 3}) {
                for (int b : new int[]{-3, 3}) {
                    for (int dy = 0; dy < ROOM_TOP; dy++) {
                        put(a, b, dy, ModBlocks.COPPER_BEAM);
                    }
                }
            }
        }

        /** La fameuse piscine du TARDIS. */
        void pool() {
            for (int a = -4; a <= 4; a++) {
                for (int b = -4; b <= 4; b++) {
                    boolean pool = Math.abs(a) <= 3 && Math.abs(b) <= 3;
                    for (int dy = -3; dy <= -1; dy++) {
                        BlockState state;
                        if (!pool) {
                            state = dy == -1 ? Blocks.PRISMARINE_BRICKS.getDefaultState() : Blocks.PRISMARINE.getDefaultState();
                        } else if (dy == -3) {
                            state = (a + b) % 2 == 0 ? Blocks.SEA_LANTERN.getDefaultState() : Blocks.PRISMARINE_BRICKS.getDefaultState();
                        } else {
                            state = Blocks.WATER.getDefaultState();
                        }
                        put(a, b, dy, state);
                    }
                }
            }
            put(-4, 0, 0, Blocks.OAK_SLAB); // plongeoir
            lamp(0, -1);
            lamp(0, 1);
        }

        void garden() {
            Random random = Random.create(center.asLong());
            Block[] flowers = {Blocks.POPPY, Blocks.DANDELION, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY,
                    Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY, Blocks.ALLIUM};
            for (int a = -4; a <= 4; a++) {
                for (int b = -4; b <= 4; b++) {
                    boolean pond = a >= 2 && a <= 3 && b >= 2 && b <= 3;
                    put(a, b, -1, pond ? Blocks.WATER.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
                    if (pond || (a == -4 && Math.abs(b) <= 1)) {
                        continue;
                    }
                    int roll = random.nextInt(10);
                    if (roll < 3) {
                        put(a, b, 0, flowers[random.nextInt(flowers.length)]);
                    } else if (roll == 3) {
                        put(a, b, 0, Blocks.FLOWERING_AZALEA);
                    } else if (roll == 4) {
                        put(a, b, 0, Blocks.GRASS);
                    }
                }
            }
            for (int a = -3; a <= 3; a += 3) {
                for (int b = -3; b <= 3; b += 3) {
                    put(a, b, ROOM_TOP, Blocks.SHROOMLIGHT);
                }
            }
        }

        void workshop() {
            Direction toCenter = forward.getOpposite();
            put(4, -3, 0, Blocks.ANVIL.getDefaultState().with(net.minecraft.block.AnvilBlock.FACING, right));
            put(4, -1, 0, Blocks.SMITHING_TABLE);
            put(4, 0, 0, Blocks.STONECUTTER.getDefaultState().with(net.minecraft.block.StonecutterBlock.FACING, toCenter));
            put(4, 1, 0, Blocks.LOOM.getDefaultState().with(net.minecraft.block.LoomBlock.FACING, toCenter));
            put(4, 3, 0, Blocks.CARTOGRAPHY_TABLE);
            put(2, 4, 0, Blocks.FLETCHING_TABLE);
            put(0, 4, 0, Blocks.CRAFTING_TABLE);
            for (int a = 0; a <= 2; a++) {
                put(a, -4, 0, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, right));
            }
            put(1, 0, 0, Blocks.GRINDSTONE);
            lamp(-2, -1);
            lamp(-2, 1);
        }

        /** Croisement : quatre poutres et une dalle lumineuse ; trois sorties. */
        void junction() {
            for (int a : new int[]{-4, 4}) {
                for (int b : new int[]{-4, 4}) {
                    for (int dy = 0; dy < ROOM_TOP; dy++) {
                        put(a, b, dy, ModBlocks.COPPER_BEAM);
                    }
                }
            }
            put(0, 0, -1, ModBlocks.HEX_LIGHT);
        }
    }

    // --- Outils --------------------------------------------------------------------------

    private static BlockPos local(BlockPos origin, Direction forward, Direction right, int a, int b, int dy) {
        return origin.offset(forward, a).offset(right, b).up(dy);
    }

    /** Boîte englobante entre deux points de l'axe, élargie de « half » sur les côtés. */
    private static BlockBox box(BlockPos from, BlockPos to, Direction right, int half, int down, int up) {
        BlockPos a = from.offset(right, half).up(down);
        BlockPos b = to.offset(right, -half).up(up);
        return BlockBox.create(a, b);
    }

    private static boolean fits(TardisData data, BlockBox candidate, boolean avoidConsoleRoom) {
        BlockPos center = TardisManager.interiorCenter(data);
        BlockBox consoleRoom = new BlockBox(center.getX() - CONSOLE_ROOM, center.getY() - 2, center.getZ() - CONSOLE_ROOM,
                center.getX() + CONSOLE_ROOM, center.getY() + 12, center.getZ() + CONSOLE_ROOM);
        if (avoidConsoleRoom && candidate.intersects(consoleRoom)) {
            return false;
        }
        if (Math.abs(candidate.getMinX() - center.getX()) > ZONE_LIMIT || Math.abs(candidate.getMaxX() - center.getX()) > ZONE_LIMIT
                || Math.abs(candidate.getMinZ() - center.getZ()) > ZONE_LIMIT || Math.abs(candidate.getMaxZ() - center.getZ()) > ZONE_LIMIT) {
            return false;
        }
        for (BlockBox other : data.getOccupied()) {
            if (candidate.intersects(other)) {
                return false;
            }
        }
        return true;
    }

    private static void set(ServerWorld world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
    }
}
