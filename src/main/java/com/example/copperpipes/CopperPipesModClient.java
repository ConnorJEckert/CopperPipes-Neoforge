package com.example.copperpipes;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.bus.api.SubscribeEvent;

@Mod(value = CopperPipesMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CopperPipesMod.MODID, value = Dist.CLIENT)
public class CopperPipesModClient {
    public CopperPipesModClient(ModContainer container) {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
    }
}
