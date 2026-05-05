package com.example.copperpipes;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CopperPipesMod.MODID);

    // Populated in FMLCommonSetupEvent — maps unwaxed block → next oxidation level
    public static final BiMap<Block, Block> OXIDATION_NEXT = HashBiMap.create();
    // Populated in FMLCommonSetupEvent — maps unwaxed block → waxed counterpart
    public static final BiMap<Block, Block> WAXABLES = HashBiMap.create();

    private static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of().strength(2.0f, 6.0f).noOcclusion();
    }

    // ── Copper Pipe ────────────────────────────────────────────────────────────
    public static final DeferredBlock<CopperPipeBlock> COPPER_PIPE =
        BLOCKS.register("copper_pipe",          () -> new CopperPipeBlock(1, false, props()));
    public static final DeferredBlock<CopperPipeBlock> EXPOSED_COPPER_PIPE =
        BLOCKS.register("exposed_copper_pipe",   () -> new CopperPipeBlock(2, false, props()));
    public static final DeferredBlock<CopperPipeBlock> WEATHERED_COPPER_PIPE =
        BLOCKS.register("weathered_copper_pipe", () -> new CopperPipeBlock(3, false, props()));
    public static final DeferredBlock<CopperPipeBlock> OXIDIZED_COPPER_PIPE =
        BLOCKS.register("oxidized_copper_pipe",  () -> new CopperPipeBlock(4, false, props()));
    public static final DeferredBlock<CopperPipeBlock> WAXED_COPPER_PIPE =
        BLOCKS.register("waxed_copper_pipe",              () -> new CopperPipeBlock(1, true, props()));
    public static final DeferredBlock<CopperPipeBlock> WAXED_EXPOSED_COPPER_PIPE =
        BLOCKS.register("waxed_exposed_copper_pipe",      () -> new CopperPipeBlock(2, true, props()));
    public static final DeferredBlock<CopperPipeBlock> WAXED_WEATHERED_COPPER_PIPE =
        BLOCKS.register("waxed_weathered_copper_pipe",    () -> new CopperPipeBlock(3, true, props()));
    public static final DeferredBlock<CopperPipeBlock> WAXED_OXIDIZED_COPPER_PIPE =
        BLOCKS.register("waxed_oxidized_copper_pipe",     () -> new CopperPipeBlock(4, true, props()));
}
