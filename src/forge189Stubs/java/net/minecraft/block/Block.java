package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public class Block {
    public String getLocalizedName() { return ""; }
    public Material getMaterial() { return Material.air; }
    public AxisAlignedBB getSelectedBoundingBox(World world, BlockPos pos) { return new AxisAlignedBB(0,0,0,1,1,1); }
    public int getMetaFromState(IBlockState state) { return 0; }
}
