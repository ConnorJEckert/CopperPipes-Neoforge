package com.example.copperpipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class CopperPipeBlock extends BaseEntityBlock {
    public static final MapCodec<CopperPipeBlock> CODEC = RecordCodecBuilder.mapCodec(
        inst -> inst.group(
            Codec.INT.fieldOf("oxidation_level").forGetter(b -> b.oxidationLevel),
            Codec.BOOL.optionalFieldOf("waxed", false).forGetter(b -> b.waxed),
            propertiesCodec()
        ).apply(inst, CopperPipeBlock::new)
    );

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED  = BlockStateProperties.POWERED;
    public static final BooleanProperty LOCKED   = BooleanProperty.create("locked");
    public static final BooleanProperty NORTH    = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH    = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST     = BlockStateProperties.EAST;
    public static final BooleanProperty WEST     = BlockStateProperties.WEST;
    public static final BooleanProperty UP       = BlockStateProperties.UP;
    public static final BooleanProperty DOWN     = BlockStateProperties.DOWN;

    final int oxidationLevel; // 1=unaffected, 2=exposed, 3=weathered, 4=oxidized
    final boolean waxed;

    public CopperPipeBlock(int oxidationLevel, boolean waxed, BlockBehaviour.Properties properties) {
        super(properties);
        this.oxidationLevel = oxidationLevel;
        this.waxed = waxed;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(LOCKED, false)
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST,  false).setValue(WEST,  false)
                .setValue(UP,    false).setValue(DOWN,  false));
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, LOCKED, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    // -------------------------------------------------------------------------
    // Placement & connections
    // -------------------------------------------------------------------------

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean powered = level.hasNeighborSignal(pos);
        Direction facing = getAutoConnectFacing(level, pos, context.getNearestLookingDirection());
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(POWERED, powered)
                .setValue(LOCKED, false)
                .setValue(NORTH, connectsTo(level, pos, Direction.NORTH))
                .setValue(SOUTH, connectsTo(level, pos, Direction.SOUTH))
                .setValue(EAST,  connectsTo(level, pos, Direction.EAST))
                .setValue(WEST,  connectsTo(level, pos, Direction.WEST))
                .setValue(UP,    connectsTo(level, pos, Direction.UP))
                .setValue(DOWN,  connectsTo(level, pos, Direction.DOWN));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(LOCKED)) return state;
        boolean connected = neighborState.getBlock() instanceof CopperPipeBlock;
        return state.setValue(dirProp(direction), connected);
    }

    static boolean connectsTo(LevelAccessor level, BlockPos pos, Direction dir) {
        return isUnlockedPipe(level.getBlockState(pos.relative(dir)));
    }

    private static boolean isUnlockedPipe(BlockState s) {
        return s.getBlock() instanceof CopperPipeBlock && !s.getValue(LOCKED);
    }

    static BooleanProperty dirProp(Direction dir) {
        return switch (dir) {
            case NORTH -> NORTH; case SOUTH -> SOUTH;
            case EAST  -> EAST;  case WEST  -> WEST;
            case UP    -> UP;    case DOWN  -> DOWN;
        };
    }

    private static Direction getAutoConnectFacing(Level level, BlockPos pos, Direction playerFacing) {
        Direction feederDir = null;
        Direction receiverDir = null;
        Direction chainEndDir = null;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (!(neighborState.getBlock() instanceof CopperPipeBlock)) continue;
            if (neighborState.getValue(LOCKED)) continue;

            Direction neighborFacing = neighborState.getValue(FACING);
            if (neighborFacing == dir.getOpposite()) {
                // Neighbor's FACING points toward this pipe — it's pushing into us
                if (feederDir == null) feederDir = dir;
            } else if (neighborFacing == dir) {
                // Neighbor's FACING points away from this pipe — it wants input from us
                if (receiverDir == null) receiverDir = dir;
            } else if (!neighborState.getValue(dirProp(neighborFacing)) && chainEndDir == null) {
                // Neighbor's FACING is perpendicular AND its output has no connection —
                // it's a chain terminal that this bend should redirect away from
                chainEndDir = dir;
            }
        }

        if (feederDir != null) {
            Direction straight = feederDir.getOpposite();
            if (receiverDir == straight) return straight;
            if (receiverDir != null) return receiverDir;
            return straight;
        }
        if (receiverDir != null) return receiverDir;
        if (chainEndDir != null) return chainEndDir.getOpposite();
        return playerFacing;
    }

    // -------------------------------------------------------------------------
    // Neighbour / redstone
    // -------------------------------------------------------------------------

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide()) {
            boolean powered = level.hasNeighborSignal(pos);
            if (powered != state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, powered), 2);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Oxidation
    // -------------------------------------------------------------------------

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return !waxed && oxidationLevel < 4;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < 0.05688889f) {
            Block next = ModBlocks.OXIDATION_NEXT.get(this);
            if (next != null) {
                preserveAndReplace(level, pos,
                    next.defaultBlockState()
                        .setValue(FACING,  state.getValue(FACING))
                        .setValue(POWERED, state.getValue(POWERED))
                        .setValue(LOCKED,  state.getValue(LOCKED)));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Interactions
    // -------------------------------------------------------------------------

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {

        if (stack.is(ModItems.PIPE_FITTING)) {
            if (!state.getValue(LOCKED)) {
                if (!level.isClientSide()) {
                    level.setBlock(pos, state.setValue(LOCKED, true), 2);
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1f, 1.2f);
                }
            } else {
                if (!level.isClientSide()) popFitting(level, pos, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        if (stack.is(Items.HONEYCOMB) && !waxed) {
            Block waxedBlock = ModBlocks.WAXABLES.get(this);
            if (waxedBlock != null) {
                if (!level.isClientSide()) {
                    preserveAndReplace(level, pos,
                        waxedBlock.defaultBlockState()
                            .setValue(FACING,  state.getValue(FACING))
                            .setValue(POWERED, state.getValue(POWERED))
                            .setValue(LOCKED,  state.getValue(LOCKED)));
                    level.levelEvent(null, 3003, pos, 0);
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        if (stack.getItem() instanceof AxeItem) {
            if (waxed) {
                Block unwaxed = ModBlocks.WAXABLES.inverse().get(this);
                if (unwaxed != null) {
                    if (!level.isClientSide()) {
                        preserveAndReplace(level, pos,
                            unwaxed.defaultBlockState()
                                .setValue(FACING,  state.getValue(FACING))
                                .setValue(POWERED, state.getValue(POWERED))
                                .setValue(LOCKED,  state.getValue(LOCKED)));
                        level.playSound(null, pos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1f, 1f);
                        level.levelEvent(null, 3004, pos, 0);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            } else if (oxidationLevel > 1) {
                Block prev = ModBlocks.OXIDATION_NEXT.inverse().get(this);
                if (prev != null) {
                    if (!level.isClientSide()) {
                        preserveAndReplace(level, pos,
                            prev.defaultBlockState()
                                .setValue(FACING,  state.getValue(FACING))
                                .setValue(POWERED, state.getValue(POWERED))
                                .setValue(LOCKED,  state.getValue(LOCKED)));
                        level.playSound(null, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1f, 1f);
                        level.levelEvent(null, 3005, pos, 0);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(LOCKED)) {
            if (!level.isClientSide()) popFitting(level, pos, player);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    private static void popFitting(Level level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        level.setBlock(pos, state.setValue(LOCKED, false), 2);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1f, 1.2f);
        if (!player.getAbilities().instabuild) {
            ItemStack drop = new ItemStack(ModItems.PIPE_FITTING.get());
            ItemEntity ie = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
            level.addFreshEntity(ie);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            if (state.getValue(LOCKED)) {
                Block.popResource(level, pos, new ItemStack(ModItems.PIPE_FITTING.get()));
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    // -------------------------------------------------------------------------
    // Preserve-and-replace: copies connection state automatically
    // -------------------------------------------------------------------------

    static void preserveAndReplace(Level level, BlockPos pos, BlockState newState) {
        CompoundTag nbt = null;
        if (level.getBlockEntity(pos) instanceof CopperPipeBlockEntity oldBe) {
            nbt = oldBe.saveWithId(level.registryAccess());
        }
        newState = newState
                .setValue(NORTH, connectsTo(level, pos, Direction.NORTH))
                .setValue(SOUTH, connectsTo(level, pos, Direction.SOUTH))
                .setValue(EAST,  connectsTo(level, pos, Direction.EAST))
                .setValue(WEST,  connectsTo(level, pos, Direction.WEST))
                .setValue(UP,    connectsTo(level, pos, Direction.UP))
                .setValue(DOWN,  connectsTo(level, pos, Direction.DOWN));
        level.setBlock(pos, newState, 3);
        if (nbt != null && level.getBlockEntity(pos) instanceof CopperPipeBlockEntity newBe) {
            newBe.loadAdditional(nbt, level.registryAccess());
            newBe.setChanged();
        }
    }

    // -------------------------------------------------------------------------
    // Misc
    // -------------------------------------------------------------------------

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopperPipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.COPPER_PIPE_BE.get(), CopperPipeBlockEntity::serverTick);
    }
}
