package com.universal.translator.mixin;

import com.universal.translator.UniversalTranslatorMod;
import com.universal.translator.engine.TranslationEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @ModifyVariable(
        method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private String universal_translator$onDrawString(String text) {
        if (text == null || text.isBlank()) return text;

        // ONLY translate text when a GUI Screen is currently open (e.g. NPC dialog, market, shop)
        // Does NOT translate in-world 3D nametags, HUD, or normal chat screen
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null || screen instanceof ChatScreen) {
            return text;
        }

        TranslationEngine engine = UniversalTranslatorMod.getEngine();
        if (engine == null || !engine.needsTranslation(text)) {
            return text;
        }

        String cached = engine.getCache().get(text.trim());
        if (cached != null && !cached.isBlank()) {
            return cached;
        }

        engine.translateAsync(text.trim(), res -> {});
        return text;
    }

    @ModifyVariable(
        method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Component universal_translator$onDrawStringComponent(Component comp) {
        if (comp == null) return comp;

        Screen screen = Minecraft.getInstance().screen;
        if (screen == null || screen instanceof ChatScreen) {
            return comp;
        }

        TranslationEngine engine = UniversalTranslatorMod.getEngine();
        if (engine == null) return comp;

        String raw = comp.getString();
        if (raw.isBlank() || !engine.needsTranslation(raw)) {
            return comp;
        }

        String cached = engine.getCache().get(raw.trim());
        if (cached != null && !cached.isBlank()) {
            return Component.literal(cached);
        }

        engine.translateAsync(raw.trim(), res -> {});
        return comp;
    }

    @ModifyVariable(
        method = "drawWordWrap(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/FormattedText;IIII)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private FormattedText universal_translator$onDrawWordWrap(FormattedText text) {
        if (text == null) return text;

        Screen screen = Minecraft.getInstance().screen;
        if (screen == null || screen instanceof ChatScreen) {
            return text;
        }

        TranslationEngine engine = UniversalTranslatorMod.getEngine();
        if (engine == null) return text;

        String raw = text.getString();
        if (raw.isBlank() || !engine.needsTranslation(raw)) {
            return text;
        }

        String cached = engine.getCache().get(raw.trim());
        if (cached != null && !cached.isBlank()) {
            return Component.literal(cached);
        }

        engine.translateAsync(raw.trim(), res -> {});
        return text;
    }
}
