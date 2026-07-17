package net.minecraft.potion;

public class Potion {
    public static Potion[] potionTypes = new Potion[32];
    public String getName() { return ""; }
    public static String getDurationString(PotionEffect effect) { return ""; }
    public boolean isBadEffect() { return false; }
}
