package net.minecraftforge.client.event;

import net.minecraft.entity.player.EntityPlayer;

public class DrawBlockHighlightEvent {
    public Object target;
    public EntityPlayer player;
    public float partialTicks;

    public void setCanceled(boolean canceled) {}
}
