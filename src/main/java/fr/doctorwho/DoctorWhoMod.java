package fr.doctorwho;

import fr.doctorwho.network.ModNetworking;
import fr.doctorwho.registry.ModBlockEntities;

import fr.doctorwho.tardis.TardisFlight;
import fr.doctorwho.registry.ModBlocks;
import fr.doctorwho.registry.ModDimensions;
import fr.doctorwho.registry.ModEntities;
import fr.doctorwho.registry.ModItemGroups;
import fr.doctorwho.registry.ModItems;
import fr.doctorwho.registry.ModSounds;
import fr.doctorwho.worldgen.ModStructures;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Point d'entrée commun (client + serveur) du mod Doctor Who.
 * <p>
 * Ne mettre ICI que du code exécutable des deux côtés : enregistrement des blocs,
 * objets, entités, dimensions, paquets réseau... Tout ce qui touche au rendu,
 * aux touches ou aux écrans va dans {@link fr.doctorwho.client.DoctorWhoClient}.
 */
public class DoctorWhoMod implements ModInitializer {

    public static final String MOD_ID = "doctorwho";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Allons-y ! Initialisation du mod Doctor Who...");

        ModSounds.register();
        ModBlocks.register();        // extérieur du TARDIS
        ModBlockEntities.register(); // état et animations du TARDIS
        ModItems.register();         // tournevis sonique, objet TARDIS
        ModItemGroups.register();    // onglet créatif
        ModNetworking.registerServer(); // panneau de vol
        TardisFlight.register();     // voyages (dématérialisation, vortex, atterrissage)
        ModDimensions.register();    // planètes : Lune, Skaro, Gallifrey
        ModEntities.register();      // Daleks, Cybermen, Anges Pleureurs, Ood, Adipose
        ModStructures.register();    // ville dalek, Citadelle de Gallifrey, base lunaire

        LOGGER.info("Mod Doctor Who prêt.");
    }

    /** Raccourci pour créer un identifiant {@code doctorwho:<path>}. */
    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}
