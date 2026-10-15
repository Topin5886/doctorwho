package fr.doctorwho.tardis.interior;

import fr.doctorwho.block.AbstractTardisDoorBlock;
import fr.doctorwho.block.FacingDecorBlock;
import fr.doctorwho.block.TardisConsoleBlock;
import fr.doctorwho.registry.ModBlocks;
import fr.doctorwho.tardis.ConsoleStyle;
import fr.doctorwho.tardis.InteriorBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.util.math.Direction;

import static fr.doctorwho.tardis.interior.ConsoleRoom.*;

/**
 * Un thème de salle de console. La salle est toujours construite dans le même ordre
 * ({@link #build}) ; chaque étape est une méthode qu'un thème peut remplacer :
 * <ol>
 *     <li>{@link #shell} : vide, sol, sous-sol, plafond et murs (blocs donnés par {@link #wallBlock},
 *     {@link #floorBlock}, {@link #ceilingBlock}...) ;</li>
 *     <li>{@link #dais} : estrade de la console ;</li>
 *     <li>{@link #gallery} et {@link #staircases} : galerie en anneau et ses deux escaliers ;</li>
 *     <li>{@link #decorate} : piliers, meubles, objets propres au thème ;</li>
 *     <li>{@link #door}, {@link #console}, {@link #rotorLights}.</li>
 * </ol>
 * Pour créer un thème : étendre cette classe (voir {@link ClassicTheme}), donner sa palette,
 * remplacer les étapes voulues, puis l'enregistrer dans {@link InteriorThemes}.
 */
public abstract class InteriorTheme {

    protected static final BlockState AIR = Blocks.AIR.getDefaultState();

    /** Identifiant du décor (voir {@code Variants.DESKTOPS}). */
    public abstract String id();

    /** Matériaux de base (servent aussi aux couloirs et aux salles ajoutées). */
    public abstract InteriorBuilder.Theme palette();

    /** Escalier assorti (marches de l'estrade et de la galerie). */
    protected abstract BlockState stairs();

    /** Garde-corps (vitre, barreaux, barrière...), raccordé à ses voisins. */
    protected abstract BlockState railing();

    /** Modèle de console posé au centre. */
    protected ConsoleStyle consoleStyle() {
        return ConsoleStyle.forDesktop(id());
    }

    /** Construit toute la salle (appelé à la première visite et quand on change de décor). */
    public final void build(RoomCanvas c) {
        shell(c);
        dais(c);
        gallery(c);
        staircases(c);
        decorate(c);
        door(c);
        console(c);
        rotorLights(c);
        ambientLights(c);
        c.finish();
    }

    /** Intensité de l'éclairage d'ambiance invisible (0 = aucun) : la salle claire du Classique en veut beaucoup. */
    protected int ambientLevel() {
        return 11;
    }

    // --- 1. Coque : vide, sol, plafond, murs ---------------------------------------------------

    protected void shell(RoomCanvas c) {
        int max = (int) Math.ceil(WALL);
        for (int x = -max; x <= max; x++) {
            for (int z = -max; z <= max; z++) {
                double r = RoomCanvas.radius(x, z);
                if (r > WALL) {
                    continue;
                }
                boolean inside = r <= RADIUS;
                boolean inner = ConsoleRoom.innerWall(x, z);
                for (int y = BOTTOM; y <= HEIGHT + 1; y++) {
                    BlockState state;
                    if (!inside) {
                        state = inner && y >= 1 && y < HEIGHT ? wallBlock(x, y, z) : outerWall();
                    } else if (y < -1) {
                        state = pitBlock(x, y, z);
                    } else if (y == -1) {
                        state = subFloorBlock(x, z);
                    } else if (y == 0) {
                        state = floorBlock(x, z);
                    } else if (y == HEIGHT) {
                        state = ceilingBlock(x, z);
                    } else if (y == HEIGHT + 1) {
                        state = outerWall();
                    } else {
                        state = AIR;
                    }
                    c.set(x, y, z, state);
                }
            }
        }
    }

    /** Bloc de la couche visible du mur (roundels, panneaux...). Par défaut : roundels du décor. */
    protected BlockState wallBlock(int x, int y, int z) {
        InteriorBuilder.Theme p = palette();
        if (p.column() != null && columnAt(x, z)) {
            return p.column().getDefaultState();
        }
        if (p.lowerWall() != null && (y == 1 || y == GALLERY_Y + 1)) {
            return p.lowerWall().getDefaultState();
        }
        if (y == GALLERY_Y) {
            return p.base().getDefaultState();
        }
        boolean lit = (y == 3 || y == 5 || y == 10 || y == 14) && (Math.abs(x) + Math.abs(z)) % 3 == 0;
        return (lit ? p.wallLit() : p.wall()).getDefaultState();
    }

    /** Hautes colonnes régulières le long du mur (décors à colonnes). */
    protected boolean columnAt(int x, int z) {
        double a = RoomCanvas.angle(x, z);
        double arc = Math.toRadians(Math.abs(((a + 15) % 30) - 15)) * RoomCanvas.radius(x, z);
        return arc < 0.6;
    }

    protected BlockState outerWall() {
        return palette().base().getDefaultState();
    }

    protected BlockState floorBlock(int x, int z) {
        return palette().floor().getDefaultState();
    }

    /** Sous le sol : lumières qui éclairent à travers les sols ajourés (grille, verre). */
    protected BlockState subFloorBlock(int x, int z) {
        boolean light = Math.floorMod(x, 3) == 0 && Math.floorMod(z, 3) == 0;
        return light ? ModBlocks.HEX_LIGHT.getDefaultState() : palette().subFloor().getDefaultState();
    }

    protected BlockState pitBlock(int x, int y, int z) {
        return palette().subFloor().getDefaultState();
    }

    protected BlockState ceilingBlock(int x, int z) {
        boolean light = Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0;
        return light ? ModBlocks.HEX_LIGHT.getDefaultState() : palette().ceiling().getDefaultState();
    }

    // --- 2. Estrade -----------------------------------------------------------------------------

    /** Estrade ronde d'un bloc de haut, bordée d'une marche. */
    protected void dais(RoomCanvas c) {
        int max = (int) Math.ceil(DAIS + 1);
        for (int x = -max; x <= max; x++) {
            for (int z = -max; z <= max; z++) {
                double r = RoomCanvas.radius(x, z);
                if (r <= DAIS) {
                    c.set(x, 1, z, daisTop(x, z));
                } else if (r <= DAIS + 1) {
                    c.setShaped(x, 1, z, stairs().with(StairsBlock.FACING, RoomCanvas.towardCenter(x, z)));
                }
            }
        }
    }

    protected BlockState daisTop(int x, int z) {
        return palette().platform().getDefaultState();
    }

    // --- 3. Galerie et escaliers ------------------------------------------------------------------

    /** Galerie en anneau au niveau {@link ConsoleRoom#GALLERY_Y}, avec garde-corps côté salle. */
    protected void gallery(RoomCanvas c) {
        int max = (int) Math.ceil(RADIUS);
        for (int x = -max; x <= max; x++) {
            for (int z = -max; z <= max; z++) {
                double r = RoomCanvas.radius(x, z);
                if (r < GALLERY_INNER || r > RADIUS) {
                    continue;
                }
                int stair = ConsoleRoom.stairHeight(x, z);
                if (stair > 0 && stair < GALLERY_Y) {
                    continue; // trémie de l'escalier
                }
                c.set(x, GALLERY_Y, z, galleryFloor(x, z));
                if (r < GALLERY_INNER + 1 && stair == 0) {
                    placeRailing(c, x, GALLERY_Y + 1, z);
                }
            }
        }
    }

    /** Pose un élément de garde-corps (par défaut : {@link #railing()}, raccordé à ses voisins). */
    protected void placeRailing(RoomCanvas c, int x, int y, int z) {
        c.setShaped(x, y, z, railing());
    }

    protected BlockState galleryFloor(int x, int z) {
        return palette().platform().getDefaultState();
    }

    /**
     * Deux escaliers droits le long du mur sud, de part et d'autre de la porte (voir {@link ConsoleRoom#onStairs}).
     * Marches toutes orientées dans le sens de la montée (jamais raccordées en angle), posées sur un massif plein.
     */
    protected void staircases(RoomCanvas c) {
        BlockState support = palette().base().getDefaultState();
        for (int side : new int[]{1, -1}) {
            Direction up = side > 0 ? Direction.EAST : Direction.WEST;
            for (int ax = STAIR_START; ax <= STAIR_LANDING; ax++) {
                int x = ax * side;
                int h = ConsoleRoom.stairHeight(x, STAIR_FIRST_Z);
                for (int z = STAIR_RAIL_Z; ConsoleRoom.inside(x, z); z++) {
                    if (h >= GALLERY_Y) {
                        if (z > STAIR_RAIL_Z) {
                            c.set(x, GALLERY_Y, z, galleryFloor(x, z)); // palier
                        }
                        continue;
                    }
                    if (z == STAIR_RAIL_Z) {
                        // Limon côté salle, surmonté du garde-corps
                        c.fill(x, 1, z, x, h, z, support);
                        placeRailing(c, x, h + 1, z);
                    } else if (z <= STAIR_LAST_Z) {
                        if (h > 1) {
                            c.fill(x, 1, z, x, h - 1, z, support);
                        }
                        c.set(x, h, z, stairs().with(StairsBlock.FACING, up));
                    } else {
                        c.fill(x, 1, z, x, h, z, support); // massif contre le mur
                    }
                }
            }
            // Garde-corps sur la galerie, au bord de la trémie (côté porte)
            for (int z = STAIR_RAIL_Z; z <= STAIR_LAST_Z + 1; z++) {
                int x = (STAIR_START - 1) * side;
                if (RoomCanvas.radius(x, z) >= GALLERY_INNER && ConsoleRoom.inside(x, z)) {
                    placeRailing(c, x, GALLERY_Y + 1, z);
                }
            }
        }
    }

    // --- 4. Décor propre au thème -------------------------------------------------------------

    /** Par défaut : piliers (et branches) en diagonale, appliques, sous la galerie et dessus. */
    protected void decorate(RoomCanvas c) {
        InteriorBuilder.Theme p = palette();
        for (int k = 0; k < 4; k++) {
            double a = Math.toRadians(45 + k * 90);
            int px = (int) Math.round(Math.cos(a) * 8.5);
            int pz = (int) Math.round(Math.sin(a) * 8.5);
            for (int y = 1; y < HEIGHT; y++) {
                c.set(px, y, pz, p.pillar().getDefaultState());
            }
            if (p.strut() != null) {
                Direction toward = RoomCanvas.towardCenter(px, pz);
                c.set(px + toward.getOffsetX(), HEIGHT - 1, pz + toward.getOffsetZ(),
                        p.strut().getDefaultState().with(FacingDecorBlock.FACING, toward));
            }
        }
        wallLamps(c, 4);
        wallLamps(c, GALLERY_Y + 4);
    }

    /** Pose un siège (on peut s'y asseoir) ; {@code looking} : direction regardée par celui qui s'assoit. */
    protected void seat(RoomCanvas c, int x, int y, int z, net.minecraft.block.Block seat, Direction looking) {
        c.set(x, y, z, seat.getDefaultState().with(FacingDecorBlock.FACING, looking));
    }

    /** Huit appliques contre le mur, à la hauteur donnée (hors escaliers). */
    protected void wallLamps(RoomCanvas c, int y) {
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(22.5 + k * 45);
            int lx = (int) Math.round(Math.cos(a) * 16);
            int lz = (int) Math.round(Math.sin(a) * 16);
            if (y < GALLERY_Y && ConsoleRoom.onStairs(lx, lz)) {
                continue;
            }
            Direction toward = RoomCanvas.towardCenter(lx, lz);
            // L'applique doit toucher le mur : on recule vers lui tant qu'il y a du vide derrière
            int bx = lx;
            int bz = lz;
            for (int step = 0; step < 4 && c.get(bx - toward.getOffsetX(), y, bz - toward.getOffsetZ()).isAir(); step++) {
                bx -= toward.getOffsetX();
                bz -= toward.getOffsetZ();
            }
            if (!c.get(bx, y, bz).isAir() || c.get(bx - toward.getOffsetX(), y, bz - toward.getOffsetZ()).isAir()) {
                continue; // pas de mur derrière, ou place occupée : pas d'applique flottante
            }
            c.set(bx, y, bz, ModBlocks.WALL_LAMP.getDefaultState().with(FacingDecorBlock.FACING, toward));
        }
    }

    // --- 5. Porte, console, lumière du rotor ----------------------------------------------------

    /** Porte intérieure dans le mur sud, avec sa niche lumineuse derrière et un encadrement. */
    protected void door(RoomCanvas c) {
        int z = EXIT_DISTANCE;
        for (int y = 1; y <= 2; y++) {
            c.set(0, y, z + 1, AIR);
            c.set(0, y, z + 2, Blocks.SEA_LANTERN.getDefaultState());
            c.set(1, y, z + 1, outerWall());
            c.set(-1, y, z + 1, outerWall());
        }
        c.set(0, 0, z + 1, palette().platform().getDefaultState());
        c.set(0, 3, z + 1, outerWall());
        doorFrame(c, z - 1);
        BlockState door = ModBlocks.TARDIS_INTERIOR_DOOR.getDefaultState()
                .with(AbstractTardisDoorBlock.FACING, Direction.NORTH);
        c.set(0, 1, z, door.with(AbstractTardisDoorBlock.HALF, DoubleBlockHalf.LOWER));
        c.set(0, 2, z, door.with(AbstractTardisDoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** Encadrement de la porte, côté salle (z = rangée juste devant le mur). */
    protected void doorFrame(RoomCanvas c, int z) {
        BlockState beam = ModBlocks.COPPER_BEAM.getDefaultState();
        for (int y = 1; y <= 4; y++) {
            c.set(1, y, z, beam);
            c.set(-1, y, z, beam);
        }
    }

    protected void console(RoomCanvas c) {
        TardisConsoleBlock.place(c.world(), ConsoleRoom.consolePos(c.center()), consoleStyle());
    }

    /** Lumières invisibles autour du rotor : la colonne éclaire vraiment la salle. */
    protected void rotorLights(RoomCanvas c) {
        BlockState light = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 13);
        int[][] around = {{4, 0}, {-4, 0}, {0, 4}, {0, -4}, {3, 3}, {-3, 3}, {3, -3}, {-3, -3}};
        for (int[] p : around) {
            c.set(p[0], 5, p[1], light);
            c.set(p[0], 10, p[1], light);
        }
    }

    /**
     * Éclairage d'ambiance : blocs de lumière invisibles en quinconce, au rez-de-chaussée et au-dessus
     * de la galerie, seulement dans l'air libre (rien ne change pour le joueur, on peut construire dessus).
     */
    protected void ambientLights(RoomCanvas c) {
        int level = ambientLevel();
        if (level <= 0) {
            return;
        }
        BlockState light = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, level);
        int max = (int) RADIUS;
        // Grille serrée, juste au-dessus des sols : la lumière reste uniforme (pas de taches d'ombre)
        for (int y : new int[]{2, GALLERY_Y + 2}) {
            for (int x = -max; x <= max; x += 3) {
                for (int z = -max; z <= max; z += 3) {
                    int zz = z + (Math.floorMod(x, 6) == 0 ? 0 : 1);
                    if (RoomCanvas.radius(x, zz) <= RADIUS - 1 && c.get(x, y, zz).isAir()) {
                        c.set(x, y, zz, light);
                    }
                }
            }
        }
    }
}
