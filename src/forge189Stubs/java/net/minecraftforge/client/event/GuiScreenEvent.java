package net.minecraftforge.client.event;

import net.minecraft.client.gui.GuiScreen;

public class GuiScreenEvent {
    public GuiScreen gui;
    public static class MouseInputEvent extends GuiScreenEvent {
        public static class Post extends MouseInputEvent {}
    }
}
