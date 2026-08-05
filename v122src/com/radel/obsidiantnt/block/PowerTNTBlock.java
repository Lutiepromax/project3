package com.radel.obsidiantnt.block;

import com.radel.obsidiantnt.logic.PowerTNTTracker;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.TNTBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public final class PowerTNTBlock extends TNTBlock {
    public PowerTNTBlock(AbstractBlock.Properties properties) {
        super(properties);
    }

    @Override
    public void catchFire(BlockState state, World world, BlockPos pos,
                          @Nullable Direction face, @Nullable LivingEntity igniter) {
        if (!world.isClientSide) {
            PowerTNTTracker.spawn(world,
                    pos.getX() + 0.5D,
                    pos.getY(),
                    pos.getZ() + 0.5D,
                    igniter,
                    80);
        }
    }

    @Override
    public void wasExploded(World world, BlockPos pos, Explosion explosion) {
        if (!world.isClientSide) {
            int fuse = 80;
            int shortenedFuse = world.random.nextInt(Math.max(1, fuse / 4)) + Math.max(1, fuse / 8);
            PowerTNTTracker.spawn(world,
                    pos.getX() + 0.5D,
                    pos.getY(),
                    pos.getZ() + 0.5D,
                    explosion.getSourceMob(),
                    shortenedFuse);
        }
    }
}
