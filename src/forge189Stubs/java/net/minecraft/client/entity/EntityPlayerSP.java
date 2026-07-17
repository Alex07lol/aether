package net.minecraft.client.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import java.util.Collection;
import java.util.ArrayList;

public class EntityPlayerSP extends EntityPlayer {
    public InventoryPlayer inventory = new InventoryPlayer();
    public Collection getActivePotionEffects() { return new ArrayList(); }
}
