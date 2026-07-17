package net.minecraft.item;

public class ItemStack {
    public String getDisplayName() { return ""; }
    public Object getItem() { return new Object(); }
    public boolean isItemDamaged() { return false; }
    public int getMaxDamage() { return 0; }
    public int getItemDamage() { return 0; }
    public ItemStack(Object block, int amount, int meta) {}
    public ItemStack() {}
}
