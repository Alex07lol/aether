package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.cosmetic.CosmeticAsset;
import dev.aether.cosmetic.CosmeticValidationResult;
import net.minecraft.client.gui.GuiScreen;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

public final class AetherCosmeticsScreen extends GuiScreen {
    private static final int KEY_ESCAPE = 1;

    private final AetherClient client;
    private final GuiScreen parent;
    private final List<AetherButton> buttons = new ArrayList<AetherButton>();
    private final List<CosmeticRow> rows = new ArrayList<CosmeticRow>();
    private String status = "Import PNG capes or select an available cosmetic.";
    private String previewAssetId;
    private BufferedImage previewImage;
    private String previewError;

    public AetherCosmeticsScreen(AetherClient client, GuiScreen parent) {
        this.client = client;
        this.parent = parent;
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        render(mouseX, mouseY);
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        render(mouseX, mouseY);
    }

    protected void mouseClicked(int mouseX, int mouseY, int clickedButton) throws IOException {
        click(mouseX, mouseY, clickedButton);
    }

    protected void func_73864_a(int mouseX, int mouseY, int clickedButton) throws IOException {
        click(mouseX, mouseY, clickedButton);
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == KEY_ESCAPE) {
            Mc189Compat.displayGuiScreen(parent);
        }
    }

    protected void func_73869_a(char typedChar, int keyCode) throws IOException {
        keyTyped(typedChar, keyCode);
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    public boolean func_73868_f() {
        return false;
    }

    private void render(int mouseX, int mouseY) {
        int width = Mc189Compat.screenWidth(this);
        int height = Mc189Compat.screenHeight(this);
        Object font = Mc189Compat.screenFontRenderer(this);

        AetherUi.background(width, height, System.currentTimeMillis());
        int left = Math.max(18, width / 2 - 230);
        int top = 24;
        int right = Math.min(width - 18, left + 460);
        int bottom = Math.min(height - 18, top + 252);
        AetherUi.panel(left, top, right, bottom);

        AetherUi.text(font, "Cosmetics", left + 14, top + 12, AetherUi.ACCENT_DARK);
        AetherUi.text(font, "Storage: " + client.cosmetics().storageDirectory().toString(), left + 14, top + 30, AetherUi.TEXT_MUTED);
        AetherUi.text(font, "Drop imports: " + client.cosmetics().importDirectory().toString(), left + 14, top + 42, AetherUi.TEXT_MUTED);

        layoutButtons(left + 14, bottom - 32);
        for (AetherButton button : buttons) {
            AetherUi.button(font, button, mouseX, mouseY);
        }

        rows.clear();
        int listLeft = left + 14;
        int listTop = top + 64;
        int listRight = left + 236;
        int y = listTop;
        List<CosmeticAsset> cosmetics = client.cosmetics().all();
        for (int i = 0; i < cosmetics.size() && y + 22 < bottom - 42; i++) {
            CosmeticRow row = new CosmeticRow(cosmetics.get(i), listLeft, y, listRight - listLeft, 20);
            rows.add(row);
            drawCosmeticRow(font, row, mouseX, mouseY);
            y += 24;
        }

        int previewLeft = left + 256;
        AetherUi.panel(previewLeft, listTop, right - 14, bottom - 44);
        AetherUi.centered(font, "Preview", previewLeft, listTop + 12, right - 14 - previewLeft, AetherUi.ACCENT_DARK);
        CosmeticAsset selected = client.cosmetics().selected();
        String selectedName = selected == null ? "No cosmetic selected" : selected.name();
        AetherUi.centered(font, selectedName, previewLeft, listTop + 34, right - 14 - previewLeft, AetherUi.TEXT_DARK);
        drawPreview(font, selected, previewLeft, listTop + 56, right - 14, bottom - 74);
        AetherUi.centered(font, status, previewLeft, bottom - 64, right - 14 - previewLeft, AetherUi.TEXT_MUTED);
    }

    private void drawCosmeticRow(Object font, CosmeticRow row, int mouseX, int mouseY) {
        CosmeticAsset selected = client.cosmetics().selected();
        boolean active = selected != null && selected.id().equals(row.asset().id());
        boolean hover = row.contains(mouseX, mouseY);
        Mc189Compat.drawRect(row.x(), row.y(), row.x() + row.width(), row.y() + row.height(), active ? 0xCC52BEEB : hover ? 0xAAFFFFFF : 0x66FFFFFF);
        AetherUi.text(font, row.asset().name(), row.x() + 8, row.y() + 6, active ? 0xFFFFFFFF : AetherUi.TEXT_DARK);
        AetherUi.text(font, row.asset().type().name(), row.x() + row.width() - 82, row.y() + 6, active ? 0xFFFFFFFF : AetherUi.TEXT_MUTED);
    }

    private void layoutButtons(final int x, final int y) {
        buttons.clear();
        buttons.add(new AetherButton("Import File", x, y, 78, 22, new ScreenAction() {
            public void run() {
                importCape();
            }
        }));
        buttons.add(new AetherButton("Import Drop", x + 86, y, 84, 22, new ScreenAction() {
            public void run() {
                importDroppedCape();
            }
        }));
        buttons.add(new AetherButton("Reload", x + 178, y, 64, 22, new ScreenAction() {
            public void run() {
                reload();
            }
        }));
        buttons.add(new AetherButton("Back", x + 250, y, 58, 22, new ScreenAction() {
            public void run() {
                Mc189Compat.displayGuiScreen(parent);
            }
        }));
    }

    private void click(int mouseX, int mouseY, int clickedButton) {
        if (clickedButton != 0) {
            return;
        }
        for (AetherButton button : buttons) {
            if (button.contains(mouseX, mouseY)) {
                button.click();
                return;
            }
        }
        for (CosmeticRow row : rows) {
            if (row.contains(mouseX, mouseY)) {
                client.cosmetics().select(row.asset().id());
                status = row.asset().name() + " selected.";
                saveQuietly();
                return;
            }
        }
    }

    private void importCape() {
        if (GraphicsEnvironment.isHeadless()) {
            status = "Native file picker is unavailable in this environment.";
            return;
        }
        Frame frame = new Frame("Import Aether Cape");
        try {
            FileDialog dialog = new FileDialog(frame, "Choose PNG Cape", FileDialog.LOAD);
            dialog.setFilenameFilter(new java.io.FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".png");
                }
            });
            dialog.setVisible(true);
            if (dialog.getFile() == null) {
                status = "Import canceled.";
                return;
            }
            File file = new File(dialog.getDirectory(), dialog.getFile());
            CosmeticValidationResult result = client.cosmetics().importCapePng(file.toPath());
            status = result.message();
            if (result.valid()) {
                saveQuietly();
            }
        } catch (IOException exception) {
            status = "Import failed: " + exception.getMessage();
        } finally {
            frame.dispose();
        }
    }

    private void importDroppedCape() {
        try {
            CosmeticValidationResult result = client.cosmetics().importNewestDroppedCape();
            status = result.message();
            if (result.valid()) {
                saveQuietly();
            }
        } catch (IOException exception) {
            status = "Drop import failed: " + exception.getMessage();
        }
    }

    private void reload() {
        try {
            client.cosmetics().load();
            previewAssetId = null;
            previewImage = null;
            previewError = null;
            status = "Cosmetics reloaded.";
        } catch (IOException exception) {
            status = "Reload failed: " + exception.getMessage();
        }
    }

    private void saveQuietly() {
        try {
            client.save();
        } catch (IOException exception) {
            status = "Selected, but config save failed: " + exception.getMessage();
        }
    }

    private void drawPreview(Object font, CosmeticAsset selected, int left, int top, int right, int bottom) {
        int width = right - left;
        if (selected == null) {
            AetherUi.centered(font, "Select a cosmetic to preview.", left, top + 24, width, AetherUi.TEXT_MUTED);
            return;
        }
        if (selected.localFile() != null) {
            loadPreviewImage(selected);
            if (previewImage != null) {
                drawImageGrid(previewImage, left + 16, top + 6, right - 16, bottom - 6);
                return;
            }
            AetherUi.centered(font, previewError == null ? "Preview unavailable." : previewError, left, top + 24, width, AetherUi.TEXT_MUTED);
            return;
        }
        drawBuiltInPreview(selected, left, top, right, bottom);
    }

    private void loadPreviewImage(CosmeticAsset selected) {
        if (selected.id().equals(previewAssetId)) {
            return;
        }
        previewAssetId = selected.id();
        previewImage = null;
        previewError = null;
        try {
            previewImage = ImageIO.read(selected.localFile().toFile());
            if (previewImage == null) {
                previewError = "PNG could not be read.";
            }
        } catch (IOException exception) {
            previewError = "Preview failed: " + exception.getMessage();
        }
    }

    private void drawImageGrid(BufferedImage image, int left, int top, int right, int bottom) {
        int columns = Math.min(32, Math.max(1, image.getWidth()));
        int rows = Math.max(1, Math.min(32, Math.round((float) image.getHeight() * (float) columns / (float) image.getWidth())));
        int cell = Math.max(2, Math.min((right - left) / columns, (bottom - top) / rows));
        int drawWidth = columns * cell;
        int drawHeight = rows * cell;
        int startX = left + (right - left - drawWidth) / 2;
        int startY = top + (bottom - top - drawHeight) / 2;
        Mc189Compat.drawRect(startX - 2, startY - 2, startX + drawWidth + 2, startY + drawHeight + 2, 0x88FFFFFF);
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                int sourceX = Math.min(image.getWidth() - 1, x * image.getWidth() / columns);
                int sourceY = Math.min(image.getHeight() - 1, y * image.getHeight() / rows);
                int argb = image.getRGB(sourceX, sourceY);
                int alpha = (argb >>> 24) & 0xFF;
                int fill = alpha < 16 ? ((x + y) % 2 == 0 ? 0x66FFFFFF : 0x44D5EFFF) : (0xFF000000 | (argb & 0x00FFFFFF));
                Mc189Compat.drawRect(startX + x * cell, startY + y * cell, startX + (x + 1) * cell, startY + (y + 1) * cell, fill);
            }
        }
    }

    private void drawBuiltInPreview(CosmeticAsset selected, int left, int top, int right, int bottom) {
        int centerX = (left + right) / 2;
        int centerY = (top + bottom) / 2;
        if (selected.type().name().equals("HALO")) {
            Mc189Compat.drawRect(centerX - 28, centerY - 18, centerX + 28, centerY - 12, 0xCC52BEEB);
            Mc189Compat.drawRect(centerX - 18, centerY - 28, centerX + 18, centerY - 22, 0xCCFFFFFF);
            Mc189Compat.drawRect(centerX - 20, centerY - 2, centerX + 20, centerY + 42, 0xAAFFFFFF);
            return;
        }
        if (selected.type().name().equals("TRAIL")) {
            for (int i = 0; i < 6; i++) {
                int size = 5 + i;
                Mc189Compat.drawRect(centerX - 42 + i * 14, centerY + i * 4, centerX - 42 + i * 14 + size, centerY + i * 4 + size, 0xAA52BEEB);
            }
            Mc189Compat.drawRect(centerX + 18, centerY - 24, centerX + 42, centerY + 36, 0xCCFFFFFF);
            return;
        }
        Mc189Compat.drawRect(centerX - 24, centerY - 34, centerX + 24, centerY + 42, 0xCC52BEEB);
        Mc189Compat.drawRect(centerX - 18, centerY - 28, centerX + 18, centerY + 36, 0xCCF5FBFF);
        Mc189Compat.drawRect(centerX - 18, centerY - 28, centerX + 18, centerY - 18, 0xAA1B8FB8);
    }

    private static final class CosmeticRow {
        private final CosmeticAsset asset;
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        CosmeticRow(CosmeticAsset asset, int x, int y, int width, int height) {
            this.asset = asset;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        CosmeticAsset asset() {
            return asset;
        }

        int x() {
            return x;
        }

        int y() {
            return y;
        }

        int width() {
            return width;
        }

        int height() {
            return height;
        }

        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}
