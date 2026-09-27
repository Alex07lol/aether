package dev.aether.forge189.ui;

/**
 * Holds geometry for a single render tick. All positions are computed in {@link ControlCenterLayout}
 * and then consumed by {@link ControlCenterRenderer}. The class is immutable – a new instance
 * is created each frame.
 */
public final class Layout {
    public final int deckX, deckY, deckW, deckH;
    public final int headerH, footerH, sidebarW, sidebarX, sidebarY, sidebarH;
    public final int listX, listY, listW, listH;
    public final int spineX, spineY, spineW, spineH;
    public final int footerY;
    public final float scroll;
    public final float maxScroll;

    public Layout(int deckX, int deckY, int deckW, int deckH,
                  int headerH, int footerH,
                  int sidebarW, int sidebarX, int sidebarY, int sidebarH,
                  int listX, int listY, int listW, int listH,
                  int spineX, int spineY, int spineW, int spineH,
                  int footerY,
                  float scroll, float maxScroll) {
        this.deckX = deckX; this.deckY = deckY; this.deckW = deckW; this.deckH = deckH;
        this.headerH = headerH; this.footerH = footerH;
        this.sidebarW = sidebarW; this.sidebarX = sidebarX; this.sidebarY = sidebarY; this.sidebarH = sidebarH;
        this.listX = listX; this.listY = listY; this.listW = listW; this.listH = listH;
        this.spineX = spineX; this.spineY = spineY; this.spineW = spineW; this.spineH = spineH;
        this.footerY = footerY;
        this.scroll = scroll; this.maxScroll = maxScroll;
    }
}