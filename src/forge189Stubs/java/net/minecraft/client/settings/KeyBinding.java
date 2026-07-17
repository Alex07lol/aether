package net.minecraft.client.settings;

public class KeyBinding {
    public KeyBinding(String description, int keyCode, String category) {}
    public static void setKeyBindState(int keyCode, boolean pressed) {}
    public boolean isPressed() { return false; }
    public boolean isKeyDown() { return false; }
    public int getKeyCode() { return 0; }
}
