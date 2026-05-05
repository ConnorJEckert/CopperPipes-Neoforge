package com.example.copperpipes;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CopperPipesMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COPPER_PIPES_TAB =
            CREATIVE_MODE_TABS.register("copper_pipes_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.copperpipes"))
                    .icon(() -> ModItems.COPPER_PIPE.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.COPPER_PIPE.get());
                        output.accept(ModItems.EXPOSED_COPPER_PIPE.get());
                        output.accept(ModItems.WEATHERED_COPPER_PIPE.get());
                        output.accept(ModItems.OXIDIZED_COPPER_PIPE.get());
                        output.accept(ModItems.WAXED_COPPER_PIPE.get());
                        output.accept(ModItems.WAXED_EXPOSED_COPPER_PIPE.get());
                        output.accept(ModItems.WAXED_WEATHERED_COPPER_PIPE.get());
                        output.accept(ModItems.WAXED_OXIDIZED_COPPER_PIPE.get());
                        output.accept(ModItems.COPPER_WRENCH.get());
                        output.accept(ModItems.PIPE_FITTING.get());
                    }).build());
}
