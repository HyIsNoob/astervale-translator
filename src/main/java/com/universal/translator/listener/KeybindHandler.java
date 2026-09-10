package com.universal.translator.listener;

import com.mojang.blaze3d.platform.InputConstants;
import com.universal.translator.gui.TranslatorConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeybindHandler {

    public static final KeyMapping KEY_OPEN_CONFIG = new KeyMapping(
            "key.universal_translator.open_config",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category.universal_translator"
    );

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEY_OPEN_CONFIG);
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        while (KEY_OPEN_CONFIG.consumeClick()) {
            Minecraft client = Minecraft.getInstance();
            if (client.screen == null) {
                client.setScreen(new TranslatorConfigScreen(null));
            }
        }
    }
}
