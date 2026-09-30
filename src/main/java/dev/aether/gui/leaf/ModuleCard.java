package dev.aether.gui.leaf;

import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

/**
 * Port of Leaf Client's {@code com.leafclient.screen.ui.ModButton} (GPLv3, see
 * docs/GUI_REBUILD.md): a 170x182 module card split into two interaction halves.
 * The top half is the card face - the module name is centered a quarter of the way
 * down, and clicking it toggles the module, with the enabled state shown as a tint.
 * The bottom half is the settings zone - Leaf draws its gear texture at (60, 110)
 * sized 50x50 inside the card, and clicking the zone opens the module's settings.
 * Hover tints whichever half the cursor is over, exactly like Leaf's
 * {@code isCover}/{@code isSubCover} pair.
 */
public final class ModuleCard extends UiComponent {

    private final String name;
    private final String categoryLabel;
    private final boolean enabled;
    private final boolean hasSettings;
    private final Runnable onToggle;
    private final Runnable onOpenSettings;

    private boolean hoverTop;
    private boolean hoverBottom;

    public ModuleCard(String name, String categoryLabel, boolean enabled, boolean hasSettings,
                      int x, int y, Runnable onToggle, Runnable onOpenSettings) {
        this.name = name;
        this.categoryLabel = categoryLabel;
        this.enabled = enabled;
        this.hasSettings = hasSettings;
        this.onToggle = onToggle;
        this.onOpenSettings = onOpenSettings;
        at(x, y).size(170, 182);
    }

    @Override
    public void render() {
        int left = gx();
        int top = gy();
        int w = gw();
        int h = gh();
        int half = h / 2;

        // Card face (top half). Leaf tints the whole texture by state; Aether tints the face.
        int faceFill = hoverTop ? AetherUi.withAlpha(AetherUi.ACCENT, 0x3D)
            : enabled ? AetherUi.withAlpha(AetherUi.GLASS_SOFT, 0xEE) : AetherUi.withAlpha(AetherUi.GLASS, 0x99);
        AetherUi.drawRoundRect(left, top, left + w, top + half, GuiScale.h(10), faceFill);
        if (enabled) {
            // The "this module is on" lamp line along the top of the card face.
            Mc189Compat.drawRect(left + GuiScale.w(10), top + GuiScale.h(6),
                left + w - GuiScale.w(10), top + GuiScale.h(8), AetherUi.ACCENT_ON);
        }

        int nameSize = AetherFont.height(AetherFont.Size.BODY);
        String shown = AetherFont.trimTo(AetherFont.Size.BODY, name, w - GuiScale.w(12));
        AetherFont.drawCenteredShadowed(AetherFont.Size.BODY, shown, left, top + (half - nameSize) / 2, w,
            enabled ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_DISABLED);

        String tag = AetherFont.trimTo(AetherFont.Size.CAPTION, categoryLabel, w - GuiScale.w(10));
        AetherFont.drawCentered(AetherFont.Size.CAPTION, tag, left, top + half - GuiScale.h(20), w, AetherUi.TEXT_SECONDARY);

        // Settings zone (bottom half): separate surface with the gear glyph in the
        // middle, mirroring Leaf's gear_small at (60, 110) inside the 170x182 card.
        int zoneFill = hoverBottom ? AetherUi.withAlpha(AetherUi.ACCENT, 0x2E)
            : AetherUi.withAlpha(AetherUi.GLASS, 0x77);
        AetherUi.drawRoundRect(left, top + half, left + w, top + h, GuiScale.h(10), zoneFill);
        AetherUi.outline(left, top, left + w, top + h, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x44));

        if (hasSettings) {
            int gear = GuiScale.h(24);
            int gearX = left + (w - gear) / 2;
            int gearY = top + half + (half - gear) / 2;
            int gearColor = hoverBottom ? AetherUi.ACCENT : AetherUi.TEXT_SECONDARY;
            AetherUi.drawCircle(gearX + gear / 2, gearY + gear / 2, gear / 2 - GuiScale.h(2), gearColor);
            AetherUi.drawCircle(gearX + gear / 2, gearY + gear / 2, gear / 4, AetherUi.GLASS);
            int cx = gearX + gear / 2;
            int cy = gearY + gear / 2;
            for (int i = 0; i < 4; i++) {
                Mc189Compat.drawRect(cx - 1, cy - gear / 2 + i * (gear / 3), cx + 1, cy - gear / 2 + GuiScale.h(3) + i * (gear / 3), gearColor);
                Mc189Compat.drawRect(cx - gear / 2 + i * (gear / 3), cy - 1, cx - gear / 2 + GuiScale.h(3) + i * (gear / 3), cy + 1, gearColor);
            }
        }
    }

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        hoverTop = contains(mouseX, mouseY) && mouseY < y + height / 2.0D;
        hoverBottom = contains(mouseX, mouseY) && mouseY >= y + height / 2.0D;
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        Minecraft.getMinecraft().getSoundHandler().playSound(
            PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
        if (mouseY < y + height / 2.0D) {
            if (onToggle != null) {
                onToggle.run();
            }
        } else if (hasSettings && onOpenSettings != null) {
            onOpenSettings.run();
        }
        return true;
    }

    @Override
    public void onMouseRelease(double mouseX, double mouseY, int button) {
        onMouseMove(mouseX, mouseY);
    }
}
