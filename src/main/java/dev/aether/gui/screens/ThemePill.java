package dev.aether.gui.screens;

import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.gui.leaf.LeafArt;
import dev.aether.theme.ThemeDefinition;
import dev.aether.forge189.AetherUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

/**
 * One theme row on the Appearance screen: Leaf's {@code select.png} pill with the theme's name
 * centred and a small swatch of the palette's own accent at its right end, so the pill previews
 * the look before it is worn. The pill is bright when its theme is the one being worn and dims
 * otherwise - the same "brightness is state" rule the rest of the port uses.
 */
final class ThemePill extends UiComponent {

    private static final int SWATCH = 16;

    private final ThemeDefinition theme;
    private final Runnable onSelect;
    private boolean worn;
    private boolean hover;

    ThemePill(ThemeDefinition theme, int x, int y, int width, int height, boolean worn, Runnable onSelect) {
        this.theme = theme;
        this.onSelect = onSelect;
        this.worn = worn;
        at(x, y).size(width, height);
    }

    public void setWorn(boolean worn) {
        this.worn = worn;
    }

    @Override
    public void render() {
        int left = gx();
        int top = gy();
        int w = gw();
        int h = gh();
        float brightness = worn ? LeafArt.BRIGHT : (hover ? LeafArt.NORMAL : LeafArt.DIMMED);
        if (hover) {
            LeafArt.drawHovered(LeafArt.SELECT, left, top, w, h, brightness);
        } else {
            LeafArt.draw(LeafArt.SELECT, left, top, w, h, brightness);
        }

        int labelSize = AetherFont.height(AetherFont.Size.BODY);
        AetherFont.drawCenteredShadowed(AetherFont.Size.BODY, theme.name(), left,
            top + (h - labelSize) / 2, w - GuiScale.w(SWATCH + 24), worn ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY);

        // The palette's own accent as a preview swatch, inset at the pill's right end.
        int accent = AetherUi.argb(theme.palette().accent());
        int swatch = GuiScale.w(SWATCH);
        int swatchLeft = left + w - swatch - GuiScale.w(14);
        int swatchTop = top + (h - swatch) / 2;
        Mc189CompatHolder.rounded(swatchLeft, swatchTop, swatch, swatch, 4, accent | 0xFF000000);
    }

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        hover = contains(mouseX, mouseY);
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        Minecraft.getMinecraft().getSoundHandler().playSound(
            PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
        if (onSelect != null) {
            onSelect.run();
        }
        return true;
    }

    @Override
    public void onMouseRelease(double mouseX, double mouseY, int button) {
        hover = contains(mouseX, mouseY);
    }

    /** Indirection so the swatch draw can stay static while the pill stays tiny. */
    private static final class Mc189CompatHolder {
        static void rounded(int x, int y, int width, int height, int radius, int color) {
            dev.aether.forge189.Mc189Compat.drawRoundedRectangle(x, y, width, height, radius, color, 0);
        }
    }
}
