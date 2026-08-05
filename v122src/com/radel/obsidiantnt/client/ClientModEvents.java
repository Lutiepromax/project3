package com.radel.obsidiantnt.client;

import com.radel.obsidiantnt.ObsidianTNTMod;
import com.radel.obsidiantnt.registry.ModBlocks;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(
        modid = ObsidianTNTMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            RenderTypeLookup.setRenderLayer(ModBlocks.DAMAGED_CRYING_OBSIDIAN_2.get(), RenderType.cutout());
            RenderTypeLookup.setRenderLayer(ModBlocks.DAMAGED_CRYING_OBSIDIAN_1.get(), RenderType.cutout());
            RenderTypeLookup.setRenderLayer(ModBlocks.DAMAGED_ANCIENT_DEBRIS_3.get(), RenderType.cutout());
            RenderTypeLookup.setRenderLayer(ModBlocks.DAMAGED_ANCIENT_DEBRIS_2.get(), RenderType.cutout());
            RenderTypeLookup.setRenderLayer(ModBlocks.DAMAGED_ANCIENT_DEBRIS_1.get(), RenderType.cutout());
        });
    }
}
