package com.universal.translator.gui;

import com.universal.translator.config.SupportedLanguage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Clean, scrollable list screen for selecting target translation languages.
 * Eliminates tedious button-clicking cycles for 17+ languages.
 */
public class LanguageSelectionScreen extends Screen {

    private final Screen parentScreen;
    private final Consumer<SupportedLanguage> onSelect;
    private SupportedLanguage selectedLanguage;
    private LanguageList languageList;
    private Button doneButton;

    private long lastClickTime = 0L;
    private SupportedLanguage lastClickedLang = null;

    public LanguageSelectionScreen(Screen parentScreen, Component title, SupportedLanguage currentLanguage, Consumer<SupportedLanguage> onSelect) {
        super(title);
        this.parentScreen = parentScreen;
        this.selectedLanguage = currentLanguage;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        super.init();

        // Scrollable list widget occupies center space
        int listTop = 38;
        int listBottomOffset = 68;
        int listHeight = this.height - listTop - listBottomOffset;
        this.languageList = new LanguageList(this.minecraft, this.width, listHeight, listTop, 22);
        this.addRenderableWidget(this.languageList);

        int btnWidth = 100;
        int btnHeight = 20;
        int btnY = this.height - 28;

        // Confirm & Done Button
        this.doneButton = this.addRenderableWidget(Button.builder(Component.literal("Select"), btn -> confirmAndClose())
                .bounds(this.width / 2 - 105, btnY, btnWidth, btnHeight)
                .build());

        // Cancel Button
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parentScreen);
            }
        }).bounds(this.width / 2 + 5, btnY, btnWidth, btnHeight).build());
    }

    private void confirmAndClose() {
        if (onSelect != null && selectedLanguage != null) {
            onSelect.accept(selectedLanguage);
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // Header Title and Subtitle
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("§7Click to select, double-click or press Select to confirm"), this.width / 2, 22, 0x888888);

        // Footer hint
        if (selectedLanguage != null) {
            String selectedInfo = "§7Selected: §b" + selectedLanguage.getEnglishName() + " §8(" + selectedLanguage.getCode().toUpperCase() + " - " + selectedLanguage.getNativeName() + ")";
            graphics.drawCenteredString(this.font, Component.literal(selectedInfo), this.width / 2, this.height - 42, 0xCCCCCC);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }

    public class LanguageList extends ObjectSelectionList<LanguageList.LanguageEntry> {

        public LanguageList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
            for (SupportedLanguage lang : SupportedLanguage.values()) {
                LanguageEntry entry = new LanguageEntry(lang);
                this.addEntry(entry);
                if (lang == selectedLanguage) {
                    this.setSelected(entry);
                }
            }
            if (this.getSelected() != null) {
                this.centerScrollOn(this.getSelected());
            }
        }

        @Override
        public int getRowWidth() {
            return 260;
        }

        public class LanguageEntry extends ObjectSelectionList.Entry<LanguageEntry> {
            private final SupportedLanguage language;

            public LanguageEntry(SupportedLanguage language) {
                this.language = language;
            }

            public SupportedLanguage getLanguage() {
                return language;
            }

            @Override
            public Component getNarration() {
                return Component.literal(language.getDisplayName());
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                boolean isCurrent = language == selectedLanguage;

                // Indicator
                String icon = isCurrent ? "§a✔ " : "  ";
                String langName = (isCurrent ? "§a§l" : (hovering ? "§e" : "§f")) + language.getEnglishName();
                String langCode = " §7[" + language.getCode().toUpperCase() + "]";
                String nativeName = " §8(" + language.getNativeName() + ")";

                String fullLabel = icon + langName + langCode + nativeName;
                graphics.drawString(minecraft.font, fullLabel, left + 4, top + (height - 8) / 2, 0xFFFFFF);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    selectedLanguage = language;
                    LanguageList.this.setSelected(this);

                    long now = System.currentTimeMillis();
                    if (lastClickedLang == language && (now - lastClickTime) < 350L) {
                        confirmAndClose();
                        return true;
                    }
                    lastClickedLang = language;
                    lastClickTime = now;
                    return true;
                }
                return false;
            }
        }
    }
}
