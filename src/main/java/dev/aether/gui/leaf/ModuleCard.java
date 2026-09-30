package dev.aether.gui.leaf;

import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.forge189.AetherUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

/**
 * Port of Leaf Client's {@code com.leafclient.screen.ui.ModButton} (GPLv3, see
 * docs/GUI_REBUILD.md): a 170x182 module card split into two interaction halves. The top half is
 * the card face - the module name is centered a quarter of the way down, and clicking it toggles
 * the module, with the enabled state shown as a brightness of the same art. The bottom half is the
 * settings zone: Leaf draws {@code gear_small.png} at (60, 110) inside the card, 50x50, shifted up
 * and left by 2 and grown by 4 while the zone is hovered, and clicking the zone opens the module's
 * settings. The gear is only drawn for modules that have settings, which is Leaf's rule for the
 * modules it special-cased.
 */
public final class ModuleCard extends UiComponent {

    /** Leaf's card size, and the size the art is authored for. */
    public static final int WIDTH = 170;
    public static final int HEIGHT = 182;

    /** Leaf's gear placement inside the card: (60, 110) at 50x50. */
    private static final int GEAR_X = 60;
    private static final int GEAR_Y = 110;
    private static final int GEAR_SIZE = 50;
    /** ...and its hover state: shifted by 2 towards the corner, grown by 4. */
    private static final int GEAR_SHIFT = 2;
    private static final int GEAR_GROW = 4;

    private final String name;
    private final boolean enabled;
    private final boolean hasSettings;
    private final Runnable onToggle;
    private final Runnable onOpenSettings;

    private boolean hoverTop;
    private boolean hoverBottom;

    public ModuleCard(String name, boolean enabled, boolean hasSettings,
                      int x, int y, Runnable onToggle, Runnable onOpenSettings) {
        this.name = name;
        this.enabled = enabled;
        this.hasSettings = hasSettings;
        this.onToggle = onToggle;
        this.onOpenSettings = onOpenSettings;
        at(x, y).size(WIDTH, HEIGHT);
    }

    @Override
    public void render() {
        int left = gx();
        int top = gy();
        int w = gw();
        int h = gh();

        // A disabled module keeps its glass but loses most of its white: the art is one texture, so
        // the state is a brightness and not a second drawing.
        float brightness = enabled ? LeafArt.BRIGHT : LeafArt.DIMMED;
        if (hoverTop || hoverBottom) {
            LeafArt.drawHovered(LeafArt.CARD, left, top, w, h, brightness);
        } else {
            LeafArt.draw(LeafArt.CARD, left, top, w, h, brightness);
        }

        // Leaf centers the name a quarter of the way down the card (h/4, minus its own font's
        // baseline padding).
        int nameSize = AetherFont.height(AetherFont.Size.BODY);
        String shown = AetherFont.trimTo(AetherFont.Size.BODY, name, w - GuiScale.w(14));
        AetherFont.drawCenteredShadowed(AetherFont.Size.BODY, shown, left, top + h / 4 - nameSize / 2, w,
            enabled ? AetherUi.TEXT_PRIMARY : AetherUi.TEXT_SECONDARY);

        if (hasSettings) {
            int shift = hoverBottom ? GEAR_SHIFT : 0;
            int grow = hoverBottom ? GEAR_GROW : 0;
            LeafArt.draw(LeafArt.GEAR,
                left + GuiScale.w(GEAR_X - shift), top + GuiScale.h(GEAR_Y - shift),
                GuiScale.w(GEAR_SIZE + grow), GuiScale.h(GEAR_SIZE + grow),
                hoverBottom ? LeafArt.BRIGHT : LeafArt.NORMAL);
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
