package com.example.copperpipes;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CopperPipesMod.MODID);

    // ── Copper Pipe ────────────────────────────────────────────────────────────
    public static final DeferredItem<BlockItem> COPPER_PIPE =
        ITEMS.registerSimpleBlockItem("copper_pipe",                   ModBlocks.COPPER_PIPE);
    public static final DeferredItem<BlockItem> EXPOSED_COPPER_PIPE =
        ITEMS.registerSimpleBlockItem("exposed_copper_pipe",           ModBlocks.EXPOSED_COPPER_PIPE);
    public static final DeferredItem<BlockItem> WEATHERED_COPPER_PIPE =
        ITEMS.registerSimpleBlockItem("weathered_copper_pipe",         ModBlocks.WEATHERED_COPPER_PIPE);
    public static final DeferredItem<BlockItem> OXIDIZED_COPPER_PIPE =
        ITEMS.registerSimpleBlockItem("oxidized_copper_pipe",          ModBlocks.OXIDIZED_COPPER_PIPE);
    public static final DeferredItem<BlockItem> WAXED_COPPER_PIPE =
        ITEMS.registerSimpleBlockItem("waxed_copper_pipe",             ModBlocks.WAXED_COPPER_PIPE);
    public static final DeferredItem<BlockItem> WAXED_EXPOSED_COPPER_PIPE =
        ITEMS.registerSimpleBlockItem("waxed_exposed_copper_pipe",     ModBlocks.WAXED_EXPOSED_COPPER_PIPE);
    public static final DeferredItem<BlockItem> WAXED_WEATHERED_COPPER_PIPE =
        ITEMS.registerSimpleBlockItem("waxed_weathered_copper_pipe",   ModBlocks.WAXED_WEATHERED_COPPER_PIPE);
    public static final DeferredItem<BlockItem> WAXED_OXIDIZED_COPPER_PIPE =
        ITEMS.registerSimpleBlockItem("waxed_oxidized_copper_pipe",    ModBlocks.WAXED_OXIDIZED_COPPER_PIPE);

    // ── Tool ───────────────────────────────────────────────────────────────────
    public static final DeferredItem<CopperWrenchItem> COPPER_WRENCH = ITEMS.register("copper_wrench",
            () -> new CopperWrenchItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> PIPE_FITTING = ITEMS.register("pipe_fitting",
            () -> new Item(new Item.Properties().stacksTo(64)));
}
