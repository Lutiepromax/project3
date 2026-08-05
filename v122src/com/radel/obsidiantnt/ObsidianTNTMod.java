package com.radel.obsidiantnt;

import com.radel.obsidiantnt.registry.ModBlocks;
import com.radel.obsidiantnt.registry.ModItems;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ObsidianTNTMod.MOD_ID)
public final class ObsidianTNTMod {
    public static final String MOD_ID = "obsidiantnt";

    public ObsidianTNTMod() {
        ModBlocks.BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModItems.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
