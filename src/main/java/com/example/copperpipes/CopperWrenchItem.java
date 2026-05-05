package com.example.copperpipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class CopperWrenchItem extends Item {

    public CopperWrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!state.hasProperty(BlockStateProperties.FACING)) return InteractionResult.PASS;
        if (!(state.getBlock() instanceof CopperPipeBlock)) return InteractionResult.PASS;
        if (state.getValue(CopperPipeBlock.LOCKED)) return InteractionResult.PASS;

        if (!level.isClientSide()) {
            Direction current = state.getValue(BlockStateProperties.FACING);
            Direction[] dirs = Direction.values();
            Direction next = null;
            for (int i = 1; i <= dirs.length; i++) {
                Direction candidate = dirs[(current.ordinal() + i) % dirs.length];
                if (isSourceDirection(level, pos, state, candidate)) continue;
                next = candidate;
                break;
            }
            if (next == null) return InteractionResult.PASS;
            level.setBlock(pos, state.setValue(BlockStateProperties.FACING, next), 3);
            // Clean up any fluid source the old output face was maintaining
            CopperPipeBlockEntity.removeTerminalOutput(level, pos.relative(current));
            level.playSound(null, pos, SoundEvents.COPPER_HIT, SoundSource.BLOCKS, 1.0f, 1.2f);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Returns true if the candidate direction has a neighbor pipe actively feeding into this pipe. */
    private static boolean isSourceDirection(Level level, BlockPos pos, BlockState state, Direction candidate) {
        if (!state.getValue(CopperPipeBlock.dirProp(candidate))) return false;
        BlockState neighbor = level.getBlockState(pos.relative(candidate));
        if (!(neighbor.getBlock() instanceof CopperPipeBlock)) return false;
        return neighbor.getValue(BlockStateProperties.FACING) == candidate.getOpposite();
    }
}
