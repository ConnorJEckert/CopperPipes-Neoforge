package com.example.copperpipes;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

@Mod(CopperPipesMod.MODID)
@EventBusSubscriber(modid = CopperPipesMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class CopperPipesMod {
    public static final String MODID = "copperpipes";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CopperPipesMod(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(CopperPipesMod::onLightningStrike);
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Oxidation progression chains
            ModBlocks.OXIDATION_NEXT.put(ModBlocks.COPPER_PIPE.get(),          ModBlocks.EXPOSED_COPPER_PIPE.get());
            ModBlocks.OXIDATION_NEXT.put(ModBlocks.EXPOSED_COPPER_PIPE.get(),  ModBlocks.WEATHERED_COPPER_PIPE.get());
            ModBlocks.OXIDATION_NEXT.put(ModBlocks.WEATHERED_COPPER_PIPE.get(),ModBlocks.OXIDIZED_COPPER_PIPE.get());

            // Wax pairings
            ModBlocks.WAXABLES.put(ModBlocks.COPPER_PIPE.get(),          ModBlocks.WAXED_COPPER_PIPE.get());
            ModBlocks.WAXABLES.put(ModBlocks.EXPOSED_COPPER_PIPE.get(),  ModBlocks.WAXED_EXPOSED_COPPER_PIPE.get());
            ModBlocks.WAXABLES.put(ModBlocks.WEATHERED_COPPER_PIPE.get(),ModBlocks.WAXED_WEATHERED_COPPER_PIPE.get());
            ModBlocks.WAXABLES.put(ModBlocks.OXIDIZED_COPPER_PIPE.get(), ModBlocks.WAXED_OXIDIZED_COPPER_PIPE.get());
        });
    }

    private static void onLightningStrike(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LightningBolt bolt) || event.getLevel().isClientSide()) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos strikePos = bolt.blockPosition();

        // Seed from direct hit and from any lightning rod at or just above the strike
        Set<BlockPos> seeds = new HashSet<>();
        for (BlockPos candidate : new BlockPos[]{strikePos, strikePos.above()}) {
            if (isPipeBlock(level.getBlockState(candidate))) {
                seeds.add(candidate);
            }
            if (level.getBlockState(candidate).is(Blocks.LIGHTNING_ROD)) {
                Direction rodFacing = level.getBlockState(candidate).getValue(BlockStateProperties.FACING);
                BlockPos attachedTo = candidate.relative(rodFacing.getOpposite());
                if (isPipeBlock(level.getBlockState(attachedTo))) seeds.add(attachedTo);
            }
        }
        if (seeds.isEmpty()) return;

        // Flood-fill connected pipeline following input→output chains
        Set<BlockPos> pipeline = new HashSet<>(seeds);
        Deque<BlockPos> queue = new ArrayDeque<>(seeds);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            BlockState currentState = level.getBlockState(current);
            if (!currentState.hasProperty(BlockStateProperties.FACING)) continue;
            Direction facing = currentState.getValue(BlockStateProperties.FACING);

            for (Direction offset : new Direction[]{facing, facing.getOpposite()}) {
                BlockPos neighbor = current.relative(offset);
                if (pipeline.contains(neighbor) || pipeline.size() >= 50) continue;
                BlockState ns = level.getBlockState(neighbor);
                if (!isPipeBlock(ns) || !ns.hasProperty(BlockStateProperties.FACING)) continue;
                if (ns.getValue(BlockStateProperties.FACING) == facing) {
                    pipeline.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        // De-oxidize each block in the pipeline by 1 stage; waxed blocks are unaffected
        for (BlockPos pos : pipeline) {
            BlockState state = level.getBlockState(pos);
            Block prev = ModBlocks.OXIDATION_NEXT.inverse().get(state.getBlock());
            if (prev == null) continue;

            BlockState newState = prev.defaultBlockState()
                    .setValue(BlockStateProperties.FACING, state.getValue(BlockStateProperties.FACING))
                    .setValue(BlockStateProperties.POWERED, state.getValue(BlockStateProperties.POWERED))
                    .setValue(CopperPipeBlock.LOCKED, state.getValue(CopperPipeBlock.LOCKED));
            CopperPipeBlock.preserveAndReplace(level, pos, newState);
        }
    }

    private static boolean isPipeBlock(BlockState state) {
        return state.getBlock() instanceof CopperPipeBlock;
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.COPPER_PIPE_BE.get(),
                (be, side) -> be.getItemHandler());
    }
}
