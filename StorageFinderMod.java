package com.example.storagefinder;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class StorageFinderMod implements ClientModInitializer {
    private static KeyMapping openGui;

    @Override
    public void onInitializeClient() {
        StorageFinderConfig.load();
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("storagefinder", "main"));

        openGui = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.storagefinder.open_gui",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_K,
                category));

        // Osobny (domyślnie nieprzypisany) klawisz dla każdego modułu — bind ustawia się w Click GUI środkowym przyciskiem myszy
        for (Module m : Modules.ALL) {
            m.keyMapping = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                    "key.storagefinder.module." + m.slug(),
                    InputConstants.Type.KEYSYM,
                    InputConstants.UNKNOWN.getValue(),
                    category));
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGui.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new ClickGuiScreen());
                }
            }
            for (Module m : Modules.ALL) {
                while (m.keyMapping.consumeClick()) {
                    if (client.screen == null) {
                        m.toggle();
                        StorageFinderConfig.save();
                    }
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(StorageScanner::tick);

        ClientTickEvents.END_CLIENT_TICK.register(Aura::tick);

        Freecam.init();
        Hud.init();
        StorageRenderer.init();
    }
}
