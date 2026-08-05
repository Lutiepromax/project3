package com.radel.obsidiantnt.registry;

import com.radel.obsidiantnt.ObsidianTNTMod;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ObsidianTNTMod.MOD_ID);

    public static final RegistryObject<Item> POWER_TNT = ITEMS.register(
            "power_tnt",
            () -> new BlockItem(ModBlocks.POWER_TNT.get(), new Item.Properties().tab(ItemGroup.TAB_REDSTONE))
    );

    private ModItems() {
    }
}
