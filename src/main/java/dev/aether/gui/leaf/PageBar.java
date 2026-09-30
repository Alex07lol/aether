package dev.aether.gui.leaf;

import dev.aether.gui.GuiScale;
import dev.aether.gui.core.UiComponent;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;

/**
 * Port of Leaf Client's {@code com.leafclient.screen.ui.ScrollBar} (GPLv3, see
 * docs/GUI_REBUILD.md): a page-based scrollbar. It knows how many items one page
 * holds ({@code amount}); scrolling advances by a whole page, the thumb height is the
 * track divided by the page count, and {@link #isScrollAble(int)} tells the screen
 * which item indexes of its list are on the current page. Leaf's wheel handling
 * ({@code onScroll}/{@code onUnScroll}) and render geometry (32-wide track, thumb at
 * {@code y + bar_point * page}) are preserved exactly.
 */
public final class PageBar extends UiComponent {

    private final int amount;
    private int size;
    private int index;
    private int page;

    public PageBar(int x, int y, int width, int height, int amount, int listSize) {
        this.amount = Math.max(1, amount);
        this.size = listSize;
        at(x, y).size(width, height);
    }

    /** Updates the list size this bar pages over (a filter change, for instance). */
    public void setListSize(int listSize) {
        this.size = Math.max(0, listSize);
        clampPage();
    }

    private int pageCount() {
        return Math.max(1, (int) Math.ceil((double) size / (double) amount));
    }

    private void clampPage() {
        int pages = pageCount();
        if (page >= pages) {
            page = pages - 1;
        }
        if (page < 0) {
            page = 0;
        }
        index = page * amount;
    }

    public void onScroll() {
        if (index + amount < size) {
            index += amount;
            page++;
        }
    }

    public void onUnScroll() {
        if (index - amount >= 0) {
            index -= amount;
            page--;
        }
    }

    public int getIndex() {
        return index;
    }

    public boolean isScrollAble(int i) {
        return i < size && i < amount * (page + 1);
    }

    @Override
    public void render() {
        int left = gx();
        int top = gy();
        int w = gw();
        int h = gh();
        int pages = pageCount();
        Mc189Compat.drawRect(left, top, left + w, top + h, AetherUi.withAlpha(AetherUi.PANEL_EDGE, 0x30));
        int barPoint = Math.max(GuiScale.h(28), h / pages);
        int barY = top + barPoint * page;
        if (barY + barPoint > top + h) {
            barY = top + h - barPoint;
        }
        // A single-page list gets a quiet full-height thumb instead of a loud one.
        int alpha = pages <= 1 ? 0x3C : 0x99;
        AetherUi.drawRoundRect(left, barY, left + w, barY + barPoint, GuiScale.w(4), AetherUi.withAlpha(AetherUi.ACCENT, alpha));
    }

    @Override
    public boolean onWheel(double mouseX, double mouseY, int delta) {
        // The screen routes the wheel here based on the cursor being over the list
        // area; the bar itself only pages.
        return false;
    }
}
