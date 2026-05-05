package com.example.copperpipes;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CopperPipesMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CopperPipeBlockEntity>> COPPER_PIPE_BE =
            BLOCK_ENTITY_TYPES.register("copper_pipe", () ->
                    BlockEntityType.Builder.of(CopperPipeBlockEntity::new,
                        ModBlocks.COPPER_PIPE.get(),
                        ModBlocks.EXPOSED_COPPER_PIPE.get(),
                        ModBlocks.WEATHERED_COPPER_PIPE.get(),
                        ModBlocks.OXIDIZED_COPPER_PIPE.get(),
                        ModBlocks.WAXED_COPPER_PIPE.get(),
                        ModBlocks.WAXED_EXPOSED_COPPER_PIPE.get(),
                        ModBlocks.WAXED_WEATHERED_COPPER_PIPE.get(),
                        ModBlocks.WAXED_OXIDIZED_COPPER_PIPE.get()
                    ).build(null));
}
