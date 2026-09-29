package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.module.impl.interface_.NotificationsModule;

import java.util.ArrayList;
import java.util.List;

/**
 * Toast queue for {@code interface.notifications}.
 * <p>
 * Entries are pushed from real client events (toggled sprint and sneak, combo milestones,
 * theme switches) and drawn as a stack in the top right corner. How long an entry lives and
 * how many can stack are the module's own settings.
 */
final class ForgeNotifications {
    private static final long FADE_MILLIS = 600L;
    private static final int WIDTH_PADDING = 14;
    private static final int ROW_HEIGHT = 18;
    private static final int ROW_GAP = 4;

    private final AetherClient client;
    private final List<Entry> entries = new ArrayList<Entry>();

    ForgeNotifications(AetherClient client) {
        this.client = client;
    }

    void push(String message) {
        if (message == null || message.trim().isEmpty() || !enabled() || !settingBool("show_toasts", true)) {
            return;
        }
        long displayMillis = clamp(settingInt("display_time", 3), 1, 10) * 1000L;
        this.entries.add(new Entry(message, System.currentTimeMillis() + displayMillis));
        int limit = clamp(settingInt("max_notifications", 5), 1, 10);
        while (this.entries.size() > limit) {
            this.entries.remove(0);
        }
    }

    /** Draws the stack, newest on top, fading out during its last moments. */
    void render(Object font, int screenWidth) {
        long now = System.currentTimeMillis();
        for (int i = this.entries.size() - 1; i >= 0; i--) {
            if (this.entries.get(i).expiresAt <= now) {
                this.entries.remove(i);
            }
        }
        if (!enabled() || !settingBool("show_toasts", true) || this.entries.isEmpty() || font == null) {
            return;
        }

        int y = 8;
        // Newest first so the stack reads top-down.
        for (int i = this.entries.size() - 1; i >= 0; i--) {
            Entry entry = this.entries.get(i);
            long remaining = entry.expiresAt - now;
            float fade = remaining >= FADE_MILLIS ? 1.0F : remaining / (float) FADE_MILLIS;
            int width = Mc189Compat.stringWidth(font, entry.text) + WIDTH_PADDING * 2;
            int left = screenWidth - 8 - width;
            int right = screenWidth - 8;
            int alpha = Math.round(0xD0 * fade);
            int background = (alpha << 24) | (AetherUi.PANEL & 0xFFFFFF);
            int edge = (Math.round(0x44 * fade) << 24) | (AetherUi.ACCENT & 0xFFFFFF);
            int textColor = AetherUi.withAlpha(AetherUi.TEXT_PRIMARY, Math.round(255 * fade));

            Mc189Compat.drawRoundedRectangle(left, y, right, y + ROW_HEIGHT, 4, background, 0);
            Mc189Compat.drawOutlinedRectangle(left, y, right, y + ROW_HEIGHT, 1, edge);
            Mc189Compat.drawStringWithShadow(font, entry.text, left + WIDTH_PADDING, y + 5, textColor);
            y += ROW_HEIGHT + ROW_GAP;
        }
    }

    private boolean enabled() {
        try {
            return this.client.modules().get(NotificationsModule.ID).state() == ModuleState.ENABLED;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean settingBool(String settingId, boolean fallback) {
        try {
            for (dev.aether.module.setting.Setting<?> setting : this.client.modules().get(NotificationsModule.ID).settings()) {
                if (settingId.equals(setting.id()) && setting.value() instanceof Boolean) {
                    return ((Boolean) setting.value()).booleanValue();
                }
            }
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
        return fallback;
    }

    private int settingInt(String settingId, int fallback) {
        try {
            for (dev.aether.module.setting.Setting<?> setting : this.client.modules().get(NotificationsModule.ID).settings()) {
                if (settingId.equals(setting.id()) && setting.value() instanceof Number) {
                    return ((Number) setting.value()).intValue();
                }
            }
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
        return fallback;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Entry {
        private final String text;
        private final long expiresAt;

        private Entry(String text, long expiresAt) {
            this.text = text;
            this.expiresAt = expiresAt;
        }
    }
}
