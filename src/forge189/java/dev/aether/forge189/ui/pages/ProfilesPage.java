package dev.aether.forge189.ui.pages;

import dev.aether.forge189.AetherClickGuiScreen;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import dev.aether.forge189.ui.Layout;
import dev.aether.forge189.ui.components.AetherButton;
import dev.aether.forge189.ui.AetherMetrics;

/**
 * Profiles page – shows a card per saved profile with apply/delete actions,
 * and a new profile input field.
 */
public final class ProfilesPage extends Page {

    private final AetherButton saveBtn = new AetherButton("Save", 0, 0, 80, 22);
    private final AetherButton newBtn = new AetherButton("New", 0, 0, 80, 22);

    @Override
    public void render(AetherClickGuiScreen screen,
                       Object font,
                       int mouseX, int mouseY,
                       Layout layout) {
        int y = layout.listY + 8;
        AetherUi.text(font, "PROFILES", layout.listX + 8, y, AetherUi.TEXT_DISABLED);
        y += 30;

        int btnX = layout.listX + layout.listW - 180;
        saveBtn.render(font, mouseX, mouseY, btnX, y);
        newBtn.render(font, mouseX, mouseY, btnX + 90, y);
        y += 30;

        String activeProfile = screen.getActiveProfile();
        for (String name : screen.getClient().profiles().names()) {
            int cardX = layout.listX + 8;
            int cardW = layout.listW - 16;
            int cardH = 34;
            boolean isActive = name.equals(activeProfile);

            int bg = isActive ? AetherUi.withAlpha(AetherUi.ACCENT, 0x26) : AetherUi.CARD;
            Mc189Compat.drawRect(cardX, y, cardX + cardW, y + cardH, bg);

            AetherUi.text(font, name, cardX + 12, y + 8,
                         isActive ? AetherUi.ACCENT : AetherUi.TEXT_PRIMARY);

            int actionX = cardX + cardW - 100;
            boolean applyHover = mouseX >= actionX && mouseX <= actionX + 46 &&
                                 mouseY >= y + 6 && mouseY <= y + 18;
            int applyBg = applyHover ? AetherUi.withAlpha(AetherUi.ACCENT, 0x33) : AetherUi.withAlpha(AetherUi.ROW_BG, 0xFF);
            Mc189Compat.drawRect(actionX, y + 6, actionX + 46, y + 18, applyBg);
            AetherUi.text(font, "APPLY", actionX + 8, y + 9, applyHover ? AetherUi.ACCENT : AetherUi.TEXT_DISABLED);

            int delX = actionX + 50;
            boolean delHover = mouseX >= delX && mouseX <= delX + 44 &&
                               mouseY >= y + 6 && mouseY <= y + 18;
            int delBg = delHover ? AetherUi.withAlpha(AetherUi.WARN, 0x33) : AetherUi.withAlpha(AetherUi.ROW_BG, 0xFF);
            Mc189Compat.drawRect(delX, y + 6, delX + 44, y + 18, delBg);
            AetherUi.text(font, "DELETE", delX + 8, y + 9, delHover ? AetherUi.WARN : AetherUi.TEXT_DISABLED);

            y += cardH + 6;
        }

        y += 4;
        AetherUi.text(font, "New profile name:", layout.listX + 8, y + 4, AetherUi.TEXT_DISABLED);
        y += 24;
        Mc189Compat.drawRect(layout.listX + 8, y, layout.listX + 200, y + 20, AetherUi.SEARCH);
        AetherUi.text(font, screen.getProfileDraftName(), layout.listX + 12, y + 4, AetherUi.TEXT_PRIMARY);

        y += 30;
        saveBtn.render(font, mouseX, mouseY, layout.listX + 8, y);
    }

    @Override
    public void handleClick(AetherClickGuiScreen screen, Layout layout,
                           int mouseX, int mouseY, int button) {
        int y = layout.listY + 8 + 30; // after PROFILES header

        // Top-right Save button
        int btnX = layout.listX + layout.listW - 180;
        if (mouseX >= btnX && mouseX <= btnX + 80 &&
            mouseY >= y && mouseY <= y + 22) {
            screen.saveProfile();
            return;
        }

        // Top-right New button
        if (mouseX >= btnX + 90 && mouseX <= btnX + 170 &&
            mouseY >= y && mouseY <= y + 22) {
            screen.newProfile();
            return;
        }

        // Profile rows
        int cardH = 34;
        int rowY = y + 30;
        for (String name : screen.getClient().profiles().names()) {
            int cardX = layout.listX + 8;
            int cardW = layout.listW - 16;
            int actionX = cardX + cardW - 100;
            if (mouseX >= actionX && mouseX <= actionX + 46 &&
                mouseY >= rowY + 6 && mouseY <= rowY + 18) {
                screen.applyProfile(name);
                return;
            }
            if (mouseX >= actionX + 50 && mouseX <= actionX + 94 &&
                mouseY >= rowY + 6 && mouseY <= rowY + 18) {
                screen.deleteProfile(name);
                return;
            }
            rowY += cardH + 6;
        }

        // Bottom Save button at y + 30 + 30 + 4 + 24 + 30
        int saveY = y + 30 + 30 + 4 + 24 + 30;
        if (mouseX >= layout.listX + 8 && mouseX <= layout.listX + 88 &&
            mouseY >= saveY && mouseY <= saveY + 22) {
            screen.saveProfile();
        }
    }
}