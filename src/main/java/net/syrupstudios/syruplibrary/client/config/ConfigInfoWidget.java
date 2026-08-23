package net.syrupstudios.syruplibrary.client.config;

import net.syrupstudios.syruplibrary.config.ConfigInfoRow;
import net.syrupstudios.syruplibrary.config.ConfigInfoStyle;
//? if >=26 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
 *///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** One non-persistent text, warning, spacer, or link row. */
final class ConfigInfoWidget extends ConfigListEntry {
    private final ConfigSectionScreen screen;
    private final ConfigInfoRow info;
    private final Component text;
    private final Button linkButton;

    ConfigInfoWidget(ConfigSectionScreen screen, ConfigInfoRow info) {
        this.screen = screen;
        this.info = info;
        this.text = ConfigText.infoText(screen.session().config().spec(), info);
        if (info.style() == ConfigInfoStyle.LINK) {
            this.linkButton = Button.builder(text, button -> openLink())
                    .bounds(0, 0, 80, 18).build();
        } else {
            this.linkButton = null;
        }
    }

    private void openLink() {
        String link = info.link().orElseThrow();
        //? if <1.21 {
        ConfirmLinkScreen.confirmLinkNow(link, screen, false);
        //?} else {
        /*ConfirmLinkScreen.confirmLinkNow(screen, link, false);
         *///?}
    }

    @Override public List<? extends GuiEventListener> children() {
        return linkButton == null ? List.of() : List.of(linkButton);
    }

    @Override public List<? extends NarratableEntry> narratables() {
        return linkButton == null ? List.of() : List.of(linkButton);
    }

    //? if >=26 {
    /*@Override
    public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        renderInfo(graphics, getX(), getX() + getWidth(), getY(), mouseX, mouseY, partialTick);
    }

    private void renderInfo(GuiGraphicsExtractor graphics, int left, int right, int top,
                            int mouseX, int mouseY, float partialTick) {
        if (linkButton != null) {
            linkButton.setX(left + 4);
            linkButton.setY(top + 7);
            linkButton.setWidth(right - left - 8);
            linkButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
        } else if (info.style() != ConfigInfoStyle.SPACER) {
            graphics.text(screen.fontInstance(), text, left + 8, top + 12, color());
        }
    }
     *///?} elif >=1.21.11 {
    /*@Override
    public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        renderInfo(graphics, getX(), getX() + getWidth(), getY(), mouseX, mouseY, partialTick);
    }

    private void renderInfo(GuiGraphics graphics, int left, int right, int top,
                            int mouseX, int mouseY, float partialTick) {
        if (linkButton != null) {
            linkButton.setX(left + 4);
            linkButton.setY(top + 7);
            linkButton.setWidth(right - left - 8);
            linkButton.render(graphics, mouseX, mouseY, partialTick);
        } else if (info.style() != ConfigInfoStyle.SPACER) {
            graphics.drawString(screen.fontInstance(), text, left + 8, top + 12, color());
        }
    }
     *///?} else {
    @Override
    public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                       int mouseX, int mouseY, boolean hovered, float partialTick) {
        if (linkButton != null) {
            linkButton.setX(left + 4);
            linkButton.setY(top + 7);
            linkButton.setWidth(width - 8);
            linkButton.render(graphics, mouseX, mouseY, partialTick);
        } else if (info.style() != ConfigInfoStyle.SPACER) {
            graphics.drawString(screen.fontInstance(), text, left + 8, top + 12, color());
        }
    }
    //?}

    private int color() {
        return switch (info.style()) {
            case WARNING -> 0xFFFFFF55;
            case ERROR -> 0xFFFF5555;
            case HELP -> 0xFF55FFFF;
            default -> 0xFFA0A0A0;
        };
    }
}
