package dev.aether.gui.components;

import dev.aether.animation.Anim;
import dev.aether.animation.AnimationMath;
import dev.aether.animation.Easing;
import dev.aether.animation.FrameClock;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;

/**
 * One cosmetic in the gallery: a rounded card with a centred thumbnail, the name, the
 * type label, an equipped badge over the thumbnail and a favourite star in the corner -
 * the modern cosmetics-manager tile, drawn from Aether's palette and animation layer.
 * <p>
 * Like {@link ModuleRow}, the card is pure presentation: what equipping does and what
 * favouriting does arrives as two callbacks, and every visual state (hover, press,
 * equipped, entrance) is driven from the frame clock, so the card behaves the same at
 * 30 and 240 FPS. The thumbnail is an already-uploaded GL texture the screen hands over,
 * which keeps texture ownership where the cache lives (the screen) rather than here.
 */
public final class CosmeticCard extends UiComponent {

    public static final int CARD_W = 220;
    public static final int CARD_H = 128;
    /** Horizontal distance between neighbouring card origins (card + gutter). */
    public static final int PITCH_X = 244;
    /** Vertical distance between neighbouring card origins. */
    public static final int PITCH_Y = 144;
    public static final int RADIUS = 10;
    /** Edge length of the square thumbnail well, in design units. */
    public static final int THUMB = 76;

    private static final float APPEAR_MILLIS = 180.0F;
    private static final float APPEAR_STAGGER_MILLIS = 18.0F;
    private static final int STAR_HIT = 26;

    private final String name;
    private final String typeLabel;
    private final boolean noneCard;
    private final Runnable onEquip;
    private final Runnable onFavorite;

    private final Anim hover = new Anim(0.0F, 120.0F, Easing.EASE_OUT_QUAD);
    private final Anim equippedAnim = new Anim(0.0F, 200.0F, Easing.EASE_OUT_CUBIC);
    private final Anim press = new Anim(0.0F, 90.0F, Easing.EASE_OUT_QUAD);
    private final Anim starHover = new Anim(0.0F, 110.0F, Easing.EASE_OUT_QUAD);
    private final Anim appear = new Anim(0.0F, APPEAR_MILLIS, Easing.EASE_OUT_CUBIC);

    private float appearDelay;
    private boolean pressed;
    private boolean favorite;
    private int index;

    /** Uploaded thumbnail texture, or -1 when the card draws its fallback well. */
    private int thumbGlId = -1;
    private int thumbPixelW;
    private int thumbPixelH;

    /** The "None" card passes {@code noneCard} and leaves {@code onFavorite} null. */
    public CosmeticCard(String name, String typeLabel, boolean noneCard,
                        Runnable onEquip, Runnable onFavorite) {
        this.name = name == null ? "" : name;
        this.typeLabel = typeLabel == null ? "" : typeLabel;
        this.noneCard = noneCard;
        this.onEquip = onEquip;
        this.onFavorite = onFavorite;
        size(CARD_W, CARD_H);
    }

    /** Places the card in the grid and starts its staggered entrance. */
    public CosmeticCard place(int x, int y, int index, boolean animated) {
        at(x, y).size(CARD_W, CARD_H);
        this.index = index;
        this.appearDelay = animated ? index * APPEAR_STAGGER_MILLIS : 0.0F;
        if (!animated) {
            this.appear.set(1.0F);
        }
        return this;
    }

    /** Advances the animated states. Called once per frame by the screen. */
    public void update() {
        if (this.appear.value() < 1.0F) {
            if (this.appearDelay > 0.0F) {
                this.appearDelay -= FrameClock.deltaMillis();
            } else {
                this.appear.target(1.0F);
                this.appear.update();
            }
        }
        this.hover.update();
        this.equippedAnim.update();
        this.press.update();
        this.starHover.update();
    }

    @Override
    public void render() {
        float appearValue = this.appear.value();
        int slide = Math.round((1.0F - appearValue) * 8.0F);
        int left = gx();
        int top = gy() + slide;
        int right = left + gw();
        int bottom = top + gh();

        float hot = this.hover.value();
        float on = this.equippedAnim.value();
        float pressedAmount = this.press.value();

        int surface = AetherUi.blend(AetherUi.CARD, AetherUi.CARD_HOVER, hot);
        surface = AetherUi.blend(surface, AetherUi.PANEL, pressedAmount * 0.5F);
        AetherUi.drawRoundRect(left, top, right, bottom, RADIUS,
            AnimationMath.scaleAlpha(surface, appearValue * (0.92F + hot * 0.08F)));

        if (on > 0.01F) {
            // The equipped state reads as an accent ring plus the faintest wash: enough to
            // find "what am I wearing" at a glance, never bright enough to glare.
            AetherUi.drawRoundRect(left, top, right, bottom, RADIUS,
                AnimationMath.scaleAlpha(AetherUi.TEXT_PRIMARY, appearValue * on * 0.05F));
            AetherUi.outline(left + 1, top + 1, right - 1, bottom - 1,
                AnimationMath.scaleAlpha(AetherUi.ACCENT, appearValue * on * 0.55F));
        }

        drawWell(left, top, appearValue, hot);
        drawThumb(left, top, appearValue);
        drawLabels(left, top, appearValue);
        if (on > 0.01F) {
            drawBadge(left, top, appearValue, on);
        }
        if (this.onFavorite != null) {
            drawStar(right, top, appearValue, hot);
        }
    }

    /** The thumbnail well: a rounded recessed square, always present under the image. */
    private void drawWell(int left, int top, float appearValue, float hot) {
        int thumbPx = GuiScale.w(THUMB);
        int wellLeft = left + (gw() - thumbPx) / 2;
        int wellTop = top + GuiScale.h(10);
        int fill = AetherUi.blend(AetherUi.TRACK, AetherUi.CARD_HOVER, hot * 0.5F);
        AetherUi.drawRoundRect(wellLeft, wellTop, wellLeft + thumbPx, wellTop + thumbPx, 8,
            AnimationMath.scaleAlpha(fill, appearValue * 0.95F));
    }

    private void drawThumb(int left, int top, float appearValue) {
        int thumbPx = GuiScale.w(THUMB);
        int wellLeft = left + (gw() - thumbPx) / 2;
        int wellTop = top + GuiScale.h(10);
        if (this.thumbGlId >= 0 && this.thumbPixelW > 0 && this.thumbPixelH > 0) {
            // Letterbox the source image inside the well so a 2:1 cape and a square halo
            // both arrive undistorted and centred.
            int inner = thumbPx - GuiScale.w(12);
            double scale = Math.min((double) inner / this.thumbPixelW, (double) inner / this.thumbPixelH);
            int drawW = (int) Math.round(this.thumbPixelW * scale);
            int drawH = (int) Math.round(this.thumbPixelH * scale);
            int drawX = wellLeft + (thumbPx - drawW) / 2;
            int drawY = wellTop + (thumbPx - drawH) / 2;
            Mc189Compat.bindTexture(this.thumbGlId);
            Mc189Compat.drawTextureQuad(drawX, drawY, drawW, drawH);
            return;
        }
        int glyphColor = AnimationMath.scaleAlpha(
            this.noneCard ? AetherUi.TEXT_DISABLED : AetherUi.TEXT_SECONDARY, appearValue);
        if (this.noneCard) {
            // A ring with a cross: "this slot wears nothing".
            int cx = wellLeft + thumbPx / 2;
            int cy = wellTop + thumbPx / 2;
            int r = Math.max(8, thumbPx / 6);
            AetherUi.outline(cx - r, cy - r, cx + r, cy + r, glyphColor);
            for (int i = -(r - 4); i <= r - 4; i++) {
                Mc189Compat.drawRect(cx + i, cy + i, cx + i + 1, cy + i + 2, glyphColor);
                Mc189Compat.drawRect(cx + i, cy - i - 1, cx + i + 1, cy - i + 1, glyphColor);
            }
        } else if (!this.name.isEmpty()) {
            AetherFont.drawCentered(AetherFont.Size.BODY,
                this.name.substring(0, 1).toUpperCase(java.util.Locale.ENGLISH),
                wellLeft, wellTop + (thumbPx - AetherFont.height(AetherFont.Size.BODY)) / 2,
                thumbPx, glyphColor);
        }
    }

    private void drawLabels(int left, int top, float appearValue) {
        int innerWidth = GuiScale.w(CARD_W - 24);
        AetherFont.drawCentered(AetherFont.Size.SMALL,
            AetherFont.trimTo(AetherFont.Size.SMALL, this.name, innerWidth),
            left, top + GuiScale.h(94), gw(), AnimationMath.scaleAlpha(AetherUi.TEXT_PRIMARY, appearValue));
        if (this.typeLabel.isEmpty()) return;
        AetherFont.drawCentered(AetherFont.Size.CAPTION,
            AetherFont.trimTo(AetherFont.Size.CAPTION, this.typeLabel, innerWidth),
            left, top + GuiScale.h(112), gw(), AnimationMath.scaleAlpha(AetherUi.TEXT_DISABLED, appearValue * 0.9F));
    }

    private void drawBadge(int left, int top, float appearValue, float on) {
        String label = this.noneCard ? "CLEARED" : "EQUIPPED";
        int thumbPx = GuiScale.w(THUMB);
        int wellLeft = left + (gw() - thumbPx) / 2;
        int wellTop = top + GuiScale.h(10);
        int width = Mc189Compat.stringWidth(null, label) + 10;
        int badgeX = wellLeft + (thumbPx - width) / 2;
        int badgeY = wellTop + thumbPx - 17;
        AetherUi.drawBadge(label, badgeX, badgeY,
            AnimationMath.scaleAlpha(AetherUi.ACCENT, appearValue * on));
    }

    private void drawStar(int right, int top, float appearValue, float hot) {
        float emphasis = Math.max(this.starHover.value(), hot * 0.35F);
        int color = this.favorite
            ? AetherUi.STAR
            : AetherUi.blend(AetherUi.TEXT_DISABLED, AetherUi.STAR, emphasis);
        AetherUi.drawStar(right - GuiScale.w(20), top + 9, AnimationMath.scaleAlpha(color, appearValue));
    }

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        this.hover.target(contains(mouseX, mouseY) ? 1.0F : 0.0F);
        this.starHover.target(this.onFavorite != null && starRect().contains(mouseX, mouseY) ? 1.0F : 0.0F);
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        this.pressed = true;
        this.press.set(0.0F);
        this.press.target(1.0F);
        if (this.onFavorite != null && starRect().contains(mouseX, mouseY)) {
            this.onFavorite.run();
            return true;
        }
        if (this.onEquip != null) {
            this.onEquip.run();
        }
        return true;
    }

    @Override
    public void onMouseRelease(double mouseX, double mouseY, int button) {
        if (this.pressed) {
            this.pressed = false;
            this.press.target(0.0F);
        }
    }

    /** Animates the equipped ring instead of snapping when the selection changes. */
    public void setEquipped(boolean value) {
        this.equippedAnim.target(value ? 1.0F : 0.0F);
    }

    public void setFavorite(boolean value) {
        this.favorite = value;
    }

    /** Hands the screen's cached thumbnail to the card; -1 clears it back to the fallback. */
    public void setThumb(int glId, int pixelWidth, int pixelHeight) {
        this.thumbGlId = glId;
        this.thumbPixelW = pixelWidth;
        this.thumbPixelH = pixelHeight;
    }

    /** Grid position of the card, so a screen maps clicks back to its asset. */
    public int index() {
        return this.index;
    }

    @Override
    public void dispose() {
        this.hover.retarget(0.0F);
        this.starHover.retarget(0.0F);
    }

    /**
     * The star's hit rectangle in content space, matching how the screen dispatches the
     * cursor into the scrolled grid (the cursor arrives already offset by the scroll).
     */
    private Rect starRect() {
        return new Rect(getX() + CARD_W - STAR_HIT - 4, getY() + 4, STAR_HIT, STAR_HIT);
    }

    private static final class Rect {
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        Rect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        boolean contains(double px, double py) {
            return px >= this.x && px <= this.x + this.width && py >= this.y && py <= this.y + this.height;
        }
    }
}
