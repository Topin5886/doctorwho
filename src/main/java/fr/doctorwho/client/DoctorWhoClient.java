package fr.doctorwho.client;

import fr.doctorwho.DoctorWhoMod;
import fr.doctorwho.client.renderer.CreatureRenderer;
import fr.doctorwho.client.renderer.TardisConsoleRenderer;
import fr.doctorwho.client.renderer.TardisExteriorModel;
import fr.doctorwho.client.renderer.TardisRenderer;
import fr.doctorwho.client.screen.DesktopChoiceScreen;
import fr.doctorwho.client.screen.FlightPanelScreen;
import fr.doctorwho.network.FlightPanelData;
import fr.doctorwho.network.ModNetworking;
import fr.doctorwho.registry.ModBlockEntities;
import fr.doctorwho.registry.ModBlocks;
import fr.doctorwho.registry.ModEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

/**
 * Point d'entrée côté client uniquement : rendus GeckoLib, écrans de la console,
 * touches du clavier, effets visuels compatibles Sodium/Iris.
 * (Le rendu du tournevis sonique est fourni par l'objet lui-même, via GeckoLib.)
 */
@Environment(EnvType.CLIENT)
public class DoctorWhoClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockEntityRendererFactories.register(ModBlockEntities.TARDIS, context -> new TardisRenderer<>(new TardisExteriorModel()));
        BlockEntityRendererFactories.register(ModBlockEntities.TARDIS_INTERIOR_DOOR,
                context -> new TardisRenderer<>(new DefaultedBlockGeoModel<>(DoctorWhoMod.id("tardis_door"))));
        BlockEntityRendererFactories.register(ModBlockEntities.TARDIS_CONSOLE, context -> new TardisConsoleRenderer());
        // Créatures
        EntityRendererRegistry.register(ModEntities.DALEK, context -> new CreatureRenderer<>(context, new CreatureRenderer.DalekModel(), 0.7f, true));
        EntityRendererRegistry.register(ModEntities.CYBERMAN, context -> new CreatureRenderer<>(context, "cyberman", 0.5f, true));
        EntityRendererRegistry.register(ModEntities.WEEPING_ANGEL, context -> new CreatureRenderer<>(context, "weeping_angel", 0.5f, false));
        EntityRendererRegistry.register(ModEntities.OOD, context -> new CreatureRenderer<>(context, "ood", 0.5f, true));
        EntityRendererRegistry.register(ModEntities.K9, context -> new CreatureRenderer<>(context, "k9", 0.4f, true));
        EntityRendererRegistry.register(ModEntities.SONTARIAN, context -> new CreatureRenderer<>(context, "sontarian", 0.5f, false));
        EntityRendererRegistry.register(ModEntities.SILENCE, context -> new CreatureRenderer<>(context, "silence", 0.4f, false));
        EntityRendererRegistry.register(ModEntities.ZYGON, context -> new CreatureRenderer<>(context, "zygon", 0.5f, false));
        EntityRendererRegistry.register(ModEntities.SLITHEEN, context -> new CreatureRenderer<>(context, "slitheen", 0.7f, false));
        EntityRendererRegistry.register(ModEntities.ADIPOSE, context -> new CreatureRenderer<>(context, "adipose", 0.3f, false));

        // La grille métallique a des trous transparents
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.METAL_GRATING, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GALLIFREY_LEAVES, RenderLayer.getCutoutMipped());

        // Le serveur demande d'ouvrir le panneau de vol (ou de le rafraîchir s'il est déjà ouvert)
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_FLIGHT_PANEL, (client, handler, buf, responseSender) -> {
            FlightPanelData data = FlightPanelData.read(buf);
            client.execute(() -> {
                if (client.currentScreen instanceof FlightPanelScreen screen) {
                    screen.update(data);
                } else {
                    client.setScreen(new FlightPanelScreen(data));
                }
            });
        });
        // Première entrée dans un TARDIS neuf : choix du décor de la salle de console
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_DESKTOP_CHOICE,
                (client, handler, buf, responseSender) -> client.execute(() -> client.setScreen(new DesktopChoiceScreen())));

        DoctorWhoMod.LOGGER.info("Initialisation du client Doctor Who.");
    }
}
