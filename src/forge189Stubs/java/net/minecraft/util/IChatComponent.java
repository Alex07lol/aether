package net.minecraft.util;

public interface IChatComponent {
    String getUnformattedText();

    String getFormattedText();

    IChatComponent createCopy();
}
