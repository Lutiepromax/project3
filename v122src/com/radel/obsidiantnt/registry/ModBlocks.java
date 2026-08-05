package com.radel.obsidiantnt.registry;

import com.radel.obsidiantnt.ObsidianTNTMod;
import com.radel.obsidiantnt.block.DamagedAbsorberBlock;
import com.radel.obsidiantnt.block.PowerTNTBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ObsidianTNTMod.MOD_ID);

    public static final RegistryObject<Block> POWER_TNT = BLOCKS.register(
            "power_tnt",
            () -> new PowerTNTBlock(AbstractBlock.Properties.copy(Blocks.TNT))
    );

    public static final RegistryObject<Block> DAMAGED_CRYING_OBSIDIAN_2 = BLOCKS.register(
            "damaged_crying_obsidian_2",
            () -> new DamagedAbsorberBlock(AbstractBlock.Properties.copy(Blocks.CRYING_OBSIDIAN))
    );

    public static final RegistryObject<Block> DAMAGED_CRYING_OBSIDIAN_1 = BLOCKS.register(
            "damaged_crying_obsidian_1",
            () -> new DamagedAbsorberBlock(AbstractBlock.Properties.copy(Blocks.CRYING_OBSIDIAN))
    );

    public static final RegistryObject<Block> DAMAGED_ANCIENT_DEBRIS_3 = BLOCKS.register(
            "damaged_ancient_debris_3",
            () -> new DamagedAbsorberBlock(AbstractBlock.Properties.copy(Blocks.ANCIENT_DEBRIS))
    );

    public static final RegistryObject<Block> DAMAGED_ANCIENT_DEBRIS_2 = BLOCKS.register(
            "damaged_ancient_debris_2",
            () -> new DamagedAbsorberBlock(AbstractBlock.Properties.copy(Blocks.ANCIENT_DEBRIS))
    );

    public static final RegistryObject<Block> DAMAGED_ANCIENT_DEBRIS_1 = BLOCKS.register(
            "damaged_ancient_debris_1",
            () -> new DamagedAbsorberBlock(AbstractBlock.Properties.copy(Blocks.ANCIENT_DEBRIS))
    );

    private ModBlocks() {
    }
}
