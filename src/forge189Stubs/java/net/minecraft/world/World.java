package net.minecraft.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.border.WorldBorder;

public class World {
    public IBlockState getBlockState(BlockPos pos) { return null; }
    public WorldBorder getWorldBorder() { return new WorldBorder(); }
}
