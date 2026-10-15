package fr.doctorwho.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.doctorwho.DoctorWhoMod;
import fr.doctorwho.network.FlightPanelData;
import fr.doctorwho.network.ModNetworking;
import fr.doctorwho.registry.ModDimensions;
import fr.doctorwho.tardis.Architecture;
import fr.doctorwho.tardis.FlightPhase;
import fr.doctorwho.tardis.TardisData;
import fr.doctorwho.tardis.Variants;
import fr.doctorwho.tardis.Waypoint;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Panneau de la console, au style « holographique » : fond sombre, liserés
 * lumineux, sceau gallifreyen animé. Trois onglets :
 * <ul>
 *     <li>VOL : destination, coordonnées, waypoints, décollage</li>
 *     <li>APPARENCE : extérieur (circuit caméléon), décor de la salle, tournevis posé dans la console</li>
 *     <li>ARCHITECTURE : couloirs et salles à ajouter à l'intérieur</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public class FlightPanelScreen extends Screen {

    private static final Identifier SEAL = DoctorWhoMod.id("textures/gui/gallifreyan_seal.png");

    private static final int PANEL_W = 340;
    private static final int PANEL_H = 278;
    private static final int RANDOM_RANGE = 1000;

    private static final int TEAL = 0x2FC4E8;
    private static final int ORANGE = 0xE8913A;
    private static final int LABEL = 0xFF7F94A8;

    private static final int TAB_FLIGHT = 0;
    private static final int TAB_APPEARANCE = 1;
    private static final int TAB_ARCHITECTURE = 2;

    private static final Map<RegistryKey<World>, Integer> PLANET_COLORS = Map.of(
            World.OVERWORLD, 0x5DBB63, World.NETHER, 0xC0392B, World.END, 0xE4E1A8,
            ModDimensions.MOON, 0xC5CAD0, ModDimensions.SKARO, 0xB8683A, ModDimensions.GALLIFREY, 0xF0913A);
    private static final Map<String, Integer> EXTERIOR_COLORS = Map.of(
            "classic", 0x5B83C9, "tenth", 0x3F6FC2, "eleventh", 0x3B7FE0,
            "twelfth", 0x35558F, "thirteenth", 0x3F8AA8, "war", 0x7D8796);
    private static final Map<String, Integer> DESKTOP_COLORS = Map.of(
            "coral", 0xE08A3C, "copper", 0xC87533, "white", 0xEDEDE8, "dark", 0x5A6170);
    private static final Map<String, Integer> SONIC_COLORS = Map.of(
            "fourth", 0xEAF4FF, "tenth", 0x5AC8FF, "eleventh", 0x5AFF78, "twelfth", 0xAA78FF,
            "thirteenth", 0xFF963C, "war", 0xFF3C3C, "river", 0x5A9CFF, "master", 0xFFC83C);
    private static final Map<String, Integer> ROOM_COLORS = Map.of(
            "bedroom", 0xD04848, "kitchen", 0xE0A040, "library", 0xA0703C, "console", 0x2FC4E8,
            "pool", 0x3FA8E8, "garden", 0x5DBB63, "workshop", 0x9A9A9A, "junction", 0xC87533);

    private FlightPanelData data;
    private int tab = TAB_FLIGHT;
    private int destinationIndex;
    private String pendingDesktop;
    private int selectedExit = -1;
    private String selectedRoom;

    private TextFieldWidget xField;
    private TextFieldWidget yField;
    private TextFieldWidget zField;
    private TextFieldWidget nameField;
    private String[] savedTexts;

    private int left;
    private int top;
    /** Haut de la zone de contenu (sous les onglets). */
    private int body;

    public FlightPanelScreen(FlightPanelData data) {
        super(Text.translatable("screen.doctorwho.flight_panel"));
        this.data = data;
        this.destinationIndex = Math.max(0, data.destinations().indexOf(data.currentWorld()));
        this.pendingDesktop = data.desktop();
    }

    /** Nouvelles données du serveur : on garde l'onglet et les coordonnées tapées. */
    public void update(FlightPanelData newData) {
        this.data = newData;
        this.pendingDesktop = newData.desktop();
        this.selectedExit = -1;
        if (xField != null) {
            savedTexts = new String[]{xField.getText(), yField.getText(), zField.getText(), ""};
        }
        clearAndInit();
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = Math.max(2, (height - PANEL_H) / 2);
        body = top + 16;

        String[] tabs = {"flight", "appearance", "architecture"};
        for (int i = 0; i < tabs.length; i++) {
            int index = i;
            addDrawableChild(new StyledButton(left + 8 + i * 110, top + 20, 104, 14,
                    Text.translatable("screen.doctorwho.flight_panel.tab." + tabs[i]), ORANGE, button -> switchTab(index))
                    .selected(tab == i));
        }
        switch (tab) {
            case TAB_APPEARANCE -> initAppearance();
            case TAB_ARCHITECTURE -> initArchitecture();
            default -> initFlight();
        }
    }

    private void switchTab(int newTab) {
        if (tab == TAB_FLIGHT && xField != null) {
            savedTexts = new String[]{xField.getText(), yField.getText(), zField.getText(), nameField.getText()};
        }
        tab = newTab;
        clearAndInit();
    }

    // --- Onglet VOL ------------------------------------------------------------------------

    private void initFlight() {
        BlockPos current = data.currentPos();
        List<RegistryKey<World>> destinations = data.destinations();
        for (int i = 0; i < destinations.size(); i++) {
            RegistryKey<World> world = destinations.get(i);
            int index = i;
            addDrawableChild(new StyledButton(left + 8 + (i % 3) * 110, body + 46 + (i / 3) * 26, 104, 22,
                    dimensionName(world), TEAL, button -> {
                        destinationIndex = index;
                        clearAndInitKeepingText();
                    }).selected(i == destinationIndex).dot(PLANET_COLORS.getOrDefault(world, 0xFFFFFF)));
        }

        xField = coordinateField(left + 8, 90, current.getX());
        yField = coordinateField(left + 106, 60, current.getY());
        zField = coordinateField(left + 174, 90, current.getZ());
        addDrawableChild(new StyledButton(left + 270, body + 114, 30, 20,
                Text.translatable("screen.doctorwho.flight_panel.here"), TEAL, button -> {
                    setCoordinates(data.currentPos());
                    destinationIndex = Math.max(0, data.destinations().indexOf(data.currentWorld()));
                    clearAndInitKeepingText();
                }));
        addDrawableChild(new StyledButton(left + 302, body + 114, 30, 20, Text.literal("⚄"), TEAL, button -> {
            Random random = Random.create();
            setCoordinates(new BlockPos(
                    data.currentPos().getX() + random.nextBetween(-RANDOM_RANGE, RANDOM_RANGE),
                    data.currentPos().getY(),
                    data.currentPos().getZ() + random.nextBetween(-RANDOM_RANGE, RANDOM_RANGE)));
        }));

        boolean landed = data.phase() == FlightPhase.LANDED;
        StyledButton takeOff = addDrawableChild(new StyledButton(left + 8, body + 144, PANEL_W - 16, 24,
                Text.translatable(landed ? "screen.doctorwho.flight_panel.take_off" : "screen.doctorwho.flight_panel.in_flight"),
                ORANGE, button -> takeOff()).big());
        takeOff.active = landed;

        nameField = new TextFieldWidget(textRenderer, left + 13, body + 196, 222, 12, Text.translatable("screen.doctorwho.flight_panel.name"));
        nameField.setDrawsBackground(false);
        nameField.setMaxLength(Waypoint.MAX_NAME_LENGTH);
        nameField.setEditableColor(0xEAF6FF);
        nameField.setPlaceholder(Text.translatable("screen.doctorwho.flight_panel.name"));
        addDrawableChild(nameField);
        StyledButton save = addDrawableChild(new StyledButton(left + 244, body + 190, 88, 20,
                Text.translatable("screen.doctorwho.flight_panel.save"), TEAL, button -> saveWaypoint()));
        save.active = data.waypoints().size() < TardisData.MAX_WAYPOINTS;

        List<Waypoint> waypoints = data.waypoints();
        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint waypoint = waypoints.get(i);
            int x = left + 8 + (i / 3) * 166;
            int y = body + 216 + (i % 3) * 15;
            int index = i;
            addDrawableChild(new StyledButton(x, y, 138, 13, Text.literal(textRenderer.trimToWidth(waypoint.name(), 110)),
                    TEAL, button -> {
                        setCoordinates(waypoint.pos());
                        int dimension = data.destinations().indexOf(waypoint.world());
                        if (dimension >= 0) {
                            destinationIndex = dimension;
                        }
                        clearAndInitKeepingText();
                    }).dot(PLANET_COLORS.getOrDefault(waypoint.world(), 0xFFFFFF)));
            addDrawableChild(new StyledButton(x + 141, y, 13, 13, Text.literal("×"), 0xE05050,
                    button -> send(ModNetworking.ACTION_DELETE_WAYPOINT, b -> b.writeVarInt(index))));
        }

        if (savedTexts != null) {
            xField.setText(savedTexts[0]);
            yField.setText(savedTexts[1]);
            zField.setText(savedTexts[2]);
            nameField.setText(savedTexts[3]);
            savedTexts = null;
        }
    }

    // --- Onglet APPARENCE ------------------------------------------------------------------

    private void initAppearance() {
        List<String> exteriors = Variants.EXTERIORS;
        for (int i = 0; i < exteriors.size(); i++) {
            String variant = exteriors.get(i);
            addDrawableChild(new StyledButton(left + 8 + (i % 3) * 110, body + 46 + (i / 3) * 24, 104, 20,
                    Variants.exteriorName(variant), TEAL,
                    button -> send(ModNetworking.ACTION_SET_EXTERIOR, b -> b.writeString(variant)))
                    .selected(variant.equals(data.exteriorVariant())).dot(EXTERIOR_COLORS.getOrDefault(variant, 0xFFFFFF)));
        }

        List<String> desktops = Variants.DESKTOPS;
        for (int i = 0; i < desktops.size(); i++) {
            String desktop = desktops.get(i);
            addDrawableChild(new StyledButton(left + 8 + i * 82, body + 114, 78, 20, Variants.desktopName(desktop), TEAL,
                    button -> {
                        pendingDesktop = desktop;
                        clearAndInit();
                    }).selected(desktop.equals(pendingDesktop)).dot(DESKTOP_COLORS.getOrDefault(desktop, 0xFFFFFF)));
        }
        StyledButton rebuild = addDrawableChild(new StyledButton(left + 8, body + 138, PANEL_W - 16, 16,
                Text.translatable("screen.doctorwho.flight_panel.rebuild", Variants.desktopName(pendingDesktop)), ORANGE,
                button -> send(ModNetworking.ACTION_SET_DESKTOP, b -> b.writeString(pendingDesktop))));
        rebuild.active = !pendingDesktop.equals(data.desktop()) && data.phase() == FlightPhase.LANDED;

        if (!data.sonicVariant().isEmpty()) {
            List<String> sonics = Variants.SONICS;
            for (int i = 0; i < sonics.size(); i++) {
                String sonic = sonics.get(i);
                addDrawableChild(new StyledButton(left + 8 + (i % 4) * 82, body + 178 + (i / 4) * 22, 78, 18,
                        Variants.sonicName(sonic), TEAL,
                        button -> send(ModNetworking.ACTION_SET_SONIC, b -> b.writeString(sonic)))
                        .selected(sonic.equals(data.sonicVariant())).dot(SONIC_COLORS.getOrDefault(sonic, 0xFFFFFF)));
            }
        }
    }

    // --- Onglet ARCHITECTURE ---------------------------------------------------------------

    private void initArchitecture() {
        List<FlightPanelData.FreeExit> exits = data.exits();
        int shown = Math.min(exits.size(), 8);
        for (int i = 0; i < shown; i++) {
            FlightPanelData.FreeExit exit = exits.get(i);
            Text label = Text.translatable("room.doctorwho." + exit.parent()).copy()
                    .append(Text.literal(" · "))
                    .append(Text.translatable("direction.doctorwho." + exit.direction().asString()));
            addDrawableChild(new StyledButton(left + 8 + (i % 2) * 166, body + 46 + (i / 2) * 20, 158, 17,
                    Text.literal(textRenderer.trimToWidth(label.getString(), 130)), TEAL, button -> {
                        selectedExit = exit.index();
                        clearAndInit();
                    }).selected(exit.index() == selectedExit).dot(ROOM_COLORS.getOrDefault(exit.parent(), 0x2FC4E8)));
        }

        List<String> types = Architecture.ROOM_TYPES;
        for (int i = 0; i < types.size(); i++) {
            String type = types.get(i);
            addDrawableChild(new StyledButton(left + 8 + (i % 4) * 82, body + 146 + (i / 4) * 24, 78, 20,
                    Text.translatable("room.doctorwho." + type), TEAL, button -> {
                        selectedRoom = type;
                        clearAndInit();
                    }).selected(type.equals(selectedRoom)).dot(ROOM_COLORS.getOrDefault(type, 0xFFFFFF)));
        }

        StyledButton build = addDrawableChild(new StyledButton(left + 8, body + 200, PANEL_W - 16, 24,
                Text.translatable("screen.doctorwho.flight_panel.build"), ORANGE, button -> {
                    int exit = selectedExit;
                    String type = selectedRoom;
                    send(ModNetworking.ACTION_BUILD_ROOM, b -> {
                        b.writeVarInt(exit);
                        b.writeString(type);
                    });
                }).big());
        build.active = selectedExit >= 0 && selectedRoom != null && data.roomCount() < Architecture.MAX_ROOMS;
    }

    // --- Outils -----------------------------------------------------------------------------

    private void clearAndInitKeepingText() {
        savedTexts = new String[]{xField.getText(), yField.getText(), zField.getText(), nameField.getText()};
        clearAndInit();
    }

    private TextFieldWidget coordinateField(int x, int boxWidth, int value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x + 16, body + 120, boxWidth - 20, 12, Text.empty());
        field.setDrawsBackground(false);
        field.setMaxLength(9);
        field.setEditableColor(0xEAF6FF);
        field.setTextPredicate(text -> text.matches("-?\\d*"));
        field.setText(Integer.toString(value));
        return addDrawableChild(field);
    }

    private void setCoordinates(BlockPos pos) {
        xField.setText(Integer.toString(pos.getX()));
        yField.setText(Integer.toString(pos.getY()));
        zField.setText(Integer.toString(pos.getZ()));
    }

    private BlockPos fieldsPos() {
        return new BlockPos(parse(xField, data.currentPos().getX()),
                parse(yField, data.currentPos().getY()),
                parse(zField, data.currentPos().getZ()));
    }

    private static int parse(TextFieldWidget field, int fallback) {
        try {
            return Integer.parseInt(field.getText());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private RegistryKey<World> destinationWorld() {
        return data.destinations().get(destinationIndex);
    }

    private static Text dimensionName(RegistryKey<World> world) {
        return Text.translatable("doctorwho.dimension." + world.getValue().getPath());
    }

    // --- Commandes envoyées au serveur -----------------------------------------------------

    private static void send(int action, Consumer<PacketByteBuf> payload) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(action);
        payload.accept(buf);
        ClientPlayNetworking.send(ModNetworking.FLIGHT_COMMAND, buf);
    }

    private void takeOff() {
        RegistryKey<World> world = destinationWorld();
        BlockPos pos = fieldsPos();
        send(ModNetworking.ACTION_TAKE_OFF, b -> {
            b.writeIdentifier(world.getValue());
            b.writeBlockPos(pos);
        });
        close();
    }

    private void saveWaypoint() {
        RegistryKey<World> world = destinationWorld();
        BlockPos pos = fieldsPos();
        String name = nameField.getText();
        send(ModNetworking.ACTION_SAVE_WAYPOINT, b -> {
            b.writeIdentifier(world.getValue());
            b.writeBlockPos(pos);
            b.writeString(name, Waypoint.MAX_NAME_LENGTH);
        });
    }

    // --- Affichage -------------------------------------------------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int right = left + PANEL_W;
        int bottom = top + PANEL_H;

        context.fillGradient(left, top, right, bottom, 0xF0121C2A, 0xF0080C14);
        StyledButton.drawBorder(context, left, top, right, bottom, 0xC0000000 | TEAL);
        context.fill(left + 1, top + 1, right - 1, top + 3, 0xFF000000 | ORANGE);

        drawSeal(context, left + PANEL_W - 70, top + 60, 170, 0.10f);
        drawSeal(context, left + 20, top + 11, 18, 0.85f);

        context.drawTextWithShadow(textRenderer, title.copy(), left + 34, top + 7, 0xFF000000 | TEAL);
        drawStatus(context, right - 10, top + 7);

        switch (tab) {
            case TAB_APPEARANCE -> renderAppearance(context);
            case TAB_ARCHITECTURE -> renderArchitecture(context);
            default -> renderFlight(context);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderFlight(DrawContext context) {
        sectionLabel(context, Text.translatable("screen.doctorwho.flight_panel.section.destination"), body + 34);
        sectionLabel(context, Text.translatable("screen.doctorwho.flight_panel.section.coordinates"), body + 102);
        coordinateBox(context, "X", left + 8, 90, xField);
        coordinateBox(context, "Y", left + 106, 60, yField);
        coordinateBox(context, "Z", left + 174, 90, zField);
        sectionLabel(context, Text.translatable("screen.doctorwho.flight_panel.waypoints",
                data.waypoints().size(), TardisData.MAX_WAYPOINTS), body + 178);
        box(context, left + 8, body + 190, left + 238, body + 210, nameField.isFocused());
    }

    private void renderAppearance(DrawContext context) {
        sectionLabel(context, Text.translatable("screen.doctorwho.flight_panel.section.exterior"), body + 34);
        sectionLabel(context, Text.translatable("screen.doctorwho.flight_panel.section.desktop"), body + 102);
        sectionLabel(context, Text.translatable("screen.doctorwho.flight_panel.section.sonic"), body + 166);
        if (data.sonicVariant().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.doctorwho.flight_panel.no_sonic"),
                    left + PANEL_W / 2, body + 196, 0xFF8A98A8);
        }
    }

    private void renderArchitecture(DrawContext context) {
        sectionLabel(context, Text.translatable("screen.doctorwho.flight_panel.section.exits", data.exits().size()), body + 34);
        if (data.exits().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.doctorwho.flight_panel.no_exit"),
                    left + PANEL_W / 2, body + 70, 0xFF8A98A8);
        }
        sectionLabel(context, Text.translatable("screen.doctorwho.flight_panel.section.room_type",
                data.roomCount(), Architecture.MAX_ROOMS), body + 134);
    }

    private void drawStatus(DrawContext context, int rightX, int y) {
        boolean landed = data.phase() == FlightPhase.LANDED;
        Text status;
        if (landed) {
            BlockPos p = data.currentPos();
            status = Text.translatable("screen.doctorwho.flight_panel.current", dimensionName(data.currentWorld()),
                    p.getX(), p.getY(), p.getZ());
        } else {
            status = Text.translatable("screen.doctorwho.flight_panel.status." + data.phase().name().toLowerCase());
        }
        int w = textRenderer.getWidth(status);
        int dotColor = landed ? 0xFF4CD07A : 0xFF000000 | ORANGE;
        StyledButton.fillCircle(context, rightX - w - 8, y + 3, 3, dotColor);
        context.drawTextWithShadow(textRenderer, status, rightX - w, y, 0xFFB8C8D8);
    }

    private void sectionLabel(DrawContext context, Text label, int y) {
        context.drawTextWithShadow(textRenderer, label, left + 8, y, LABEL);
        context.fill(left + 8 + textRenderer.getWidth(label) + 6, y + 4, left + PANEL_W - 8, y + 5, 0x402FC4E8);
    }

    private void coordinateBox(DrawContext context, String axis, int x, int boxWidth, TextFieldWidget field) {
        box(context, x, body + 114, x + boxWidth, body + 134, field.isFocused());
        context.drawTextWithShadow(textRenderer, axis, x + 5, body + 120, 0xFF000000 | ORANGE);
    }

    private static void box(DrawContext context, int x0, int y0, int x1, int y1, boolean focused) {
        context.fill(x0, y0, x1, y1, 0xC0060A10);
        StyledButton.drawBorder(context, x0, y0, x1, y1, focused ? 0xFF000000 | TEAL : 0x602FC4E8);
    }

    private void drawSeal(DrawContext context, int cx, int cy, int size, float alpha) {
        float angle = (System.currentTimeMillis() % 120_000L) / 120_000f * 360f;
        context.getMatrices().push();
        context.getMatrices().translate(cx, cy, 0);
        context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
        RenderSystem.enableBlend();
        context.setShaderColor(0.45f, 0.85f, 1.0f, alpha);
        context.drawTexture(SEAL, -size / 2, -size / 2, size, size, 0, 0, 256, 256, 256, 256);
        context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        context.getMatrices().pop();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
