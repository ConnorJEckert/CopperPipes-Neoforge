package com.example.copperpipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.HashSet;
import java.util.Set;

public class CopperPipeBlockEntity extends BlockEntity implements GameEventListener {
    static final int TRANSFER_COOLDOWN = 8;

    final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private int transferCooldown = 0;
    PipeFluid pipeFluid = PipeFluid.NONE;
    private int vibrationCooldown = 0;

    // Blocks re-entry: level.gameEvent() dispatches synchronously, so the terminal pipe
    // would immediately re-trigger handleGameEvent on itself and loop infinitely.
    private static boolean propagating = false;

    private final DynamicGameEventListener<CopperPipeBlockEntity> dynamicGameEventListener =
            new DynamicGameEventListener<>(this);

    public CopperPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COPPER_PIPE_BE.get(), pos, state);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    // -------------------------------------------------------------------------
    // GameEventListener — vibration pickup
    // -------------------------------------------------------------------------

    @Override
    public PositionSource getListenerSource() {
        return new BlockPositionSource(worldPosition);
    }

    @Override
    public int getListenerRadius() {
        return 8;
    }

    @Override
    public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 exactPos) {
        if (propagating) return false;
        if (vibrationCooldown > 0) return false;
        BlockState state = getBlockState();
        if (state.getValue(CopperPipeBlock.POWERED)) return false;
        Direction outputDir = state.getValue(CopperPipeBlock.FACING);
        // Only receive when the input opening (back face) is open to air
        if (!level.getBlockState(worldPosition.relative(outputDir.getOpposite())).isAir()) return false;

        propagating = true;
        try {
            propagateVibration(level, worldPosition, event, context, new HashSet<>());
        } finally {
            propagating = false;
        }
        vibrationCooldown = 20;
        return true;
    }

    private static void propagateVibration(ServerLevel level, BlockPos pos, Holder<GameEvent> event,
            GameEvent.Context context, Set<BlockPos> visited) {
        if (!visited.add(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(CopperPipeBlock.FACING)) return;

        Direction outputDir = state.getValue(CopperPipeBlock.FACING);
        BlockPos nextPos = pos.relative(outputDir);
        BlockState nextState = level.getBlockState(nextPos);

        if (nextState.getBlock() instanceof CopperPipeBlock && !nextState.getValue(CopperPipeBlock.POWERED)) {
            propagateVibration(level, nextPos, event, context, visited);
        } else {
            // Terminal — emit the vibration here, affecting nearby sculk and Wardens
            level.gameEvent(event, Vec3.atCenterOf(nextPos), context);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel sl) {
            dynamicGameEventListener.add(sl);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel sl) {
            dynamicGameEventListener.remove(sl);
        }
    }

    // -------------------------------------------------------------------------
    // Tick
    // -------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, CopperPipeBlockEntity be) {
        tickFluid(level, pos, state, be);

        if (be.vibrationCooldown > 0) be.vibrationCooldown--;

        if (state.getValue(CopperPipeBlock.POWERED)) return;

        if (be.transferCooldown > 0) {
            be.transferCooldown--;
            return;
        }

        Direction outputDir = state.getValue(CopperPipeBlock.FACING);

        if (!be.itemHandler.getStackInSlot(0).isEmpty()) {
            if (be.pushItem(level, pos, outputDir)) {
                be.transferCooldown = TRANSFER_COOLDOWN;
                return;
            }
        }

        if (be.itemHandler.getStackInSlot(0).isEmpty()) {
            if (be.pullItem(level, pos, state, outputDir)) {
                be.transferCooldown = TRANSFER_COOLDOWN;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Fluid propagation
    // -------------------------------------------------------------------------

    static void tickFluid(Level level, BlockPos pos, BlockState state, CopperPipeBlockEntity be) {
        Direction outputDir = state.getValue(BlockStateProperties.FACING);

        // Powered = closed; clear fluid and clean up any terminal output
        if (state.getValue(CopperPipeBlock.POWERED)) {
            be.pipeFluid = PipeFluid.NONE;
            BlockPos outputPos = pos.relative(outputDir);
            if (!isFluidPipe(level, outputPos)) removeTerminalOutput(level, outputPos);
            return;
        }

        PipeFluid bestFluid = PipeFluid.NONE;

        for (Direction dir : Direction.values()) {
            if (dir == outputDir) continue;
            BlockPos neighborPos = pos.relative(dir);

            PipeFluid src = detectFluidSource(level, neighborPos);
            if (src != PipeFluid.NONE) {
                bestFluid = src;
                break;
            }

            BlockState neighborState = level.getBlockState(neighborPos);
            if (!neighborState.hasProperty(BlockStateProperties.FACING)) continue;
            if (neighborState.getValue(BlockStateProperties.FACING) != dir.getOpposite()) continue;

            PipeFluid upFluid = getEntityFluid(level.getBlockEntity(neighborPos));
            if (upFluid != PipeFluid.NONE) {
                bestFluid = upFluid;
                break;
            }
        }

        be.pipeFluid = bestFluid;

        BlockPos outputPos = pos.relative(outputDir);
        boolean isTerminal = !isFluidPipe(level, outputPos);

        if (be.pipeFluid == PipeFluid.NONE) {
            if (isTerminal) removeTerminalOutput(level, outputPos);
            return;
        }

        if (level.random.nextInt(20) == 0) {
            applyLeakEffects(level, pos, be.pipeFluid);
        }

        if (isTerminal) {
            applyTerminalOutput(level, outputPos, be.pipeFluid);
            applyTerminalParticles(level, outputPos, be.pipeFluid);
        }
    }

    // Called at every pipe carrying fluid — effects "leak" through the pipeline
    static void applyLeakEffects(Level level, BlockPos pos, PipeFluid fluid) {
        switch (fluid) {
            case WATER -> {
                if (level instanceof ServerLevel sl) {
                    double cx = pos.getX() + 0.5, cz = pos.getZ() + 0.5;
                    sl.sendParticles(ParticleTypes.DRIPPING_WATER, cx, pos.getY() + 0.2, cz, 1, 0.4, 0.2, 0.4, 0.0);
                }
                for (BlockPos adj : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 0, 2))) {
                    BlockState bs = level.getBlockState(adj);
                    if (bs.is(Blocks.FARMLAND) && bs.getValue(BlockStateProperties.MOISTURE) < 7)
                        level.setBlock(adj, bs.setValue(BlockStateProperties.MOISTURE, 7), 2);
                }
                level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(1.5))
                     .forEach(e -> { if (e.isOnFire()) e.clearFire(); });
            }
            case LAVA -> {
                if (level instanceof ServerLevel sl) {
                    double cx = pos.getX() + 0.5, cz = pos.getZ() + 0.5;
                    sl.sendParticles(ParticleTypes.DRIPPING_LAVA, cx, pos.getY() + 0.2, cz, 1, 0.4, 0.2, 0.4, 0.0);
                }
                level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(1.5))
                     .forEach(e -> e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), 100)));
            }
            case SMOKE -> {
                if (level instanceof ServerLevel sl) {
                    double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
                    sl.sendParticles(ParticleTypes.SMOKE, cx, cy, cz, 1, 0.2, 0.2, 0.2, 0.005);
                }
            }
            default -> {}
        }
    }

    static void removeTerminalOutput(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        var fluidState = state.getFluidState();
        if (!fluidState.isSource() || (!fluidState.is(Fluids.WATER) && !fluidState.is(Fluids.LAVA))) return;

        PipeFluid placedFluid = fluidState.is(Fluids.WATER) ? PipeFluid.WATER : PipeFluid.LAVA;

        // Don't remove if an active pipe carrying the SAME fluid is pointing here — it's maintaining it
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            if (getEntityFluid(level.getBlockEntity(neighborPos)) != placedFluid) continue;
            BlockState ns = level.getBlockState(neighborPos);
            if (ns.hasProperty(BlockStateProperties.FACING) && ns.getValue(BlockStateProperties.FACING) == dir.getOpposite()) {
                return;
            }
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    static void applyTerminalOutput(Level level, BlockPos outputPos, PipeFluid fluid) {
        BlockState outState = level.getBlockState(outputPos);
        var existingFluid = outState.getFluidState();

        if (!outState.isAir() && !existingFluid.is(FluidTags.WATER) && !existingFluid.is(FluidTags.LAVA) && !outState.is(Blocks.CAULDRON)) return;

        if (fluid == PipeFluid.WATER) {
            if (!existingFluid.isSource() || !existingFluid.is(Fluids.WATER)) {
                level.setBlock(outputPos, Blocks.WATER.defaultBlockState(), 3);
            }
        } else if (fluid == PipeFluid.LAVA) {
            if (outState.is(Blocks.CAULDRON)) {
                level.setBlock(outputPos, Blocks.LAVA_CAULDRON.defaultBlockState(), 3);
            } else if (!existingFluid.isSource() || !existingFluid.is(Fluids.LAVA)) {
                level.setBlock(outputPos, Blocks.LAVA.defaultBlockState(), 3);
            }
        }
    }

    static void applyTerminalParticles(Level level, BlockPos outputPos, PipeFluid fluid) {
        if (!(level instanceof ServerLevel sl)) return;
        double cx = outputPos.getX() + 0.5, cy = outputPos.getY() + 0.5, cz = outputPos.getZ() + 0.5;
        switch (fluid) {
            case LAVA -> sl.sendParticles(ParticleTypes.LAVA, cx, cy, cz, 1, 0.2, 0.2, 0.2, 0.0);
            case SMOKE -> sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, cx, cy + 0.5, cz, 2, 0.2, 0.05, 0.2, 0.01);
            default -> {}
        }
    }

    static PipeFluid detectFluidSource(Level level, BlockPos pos) {
        if (level.getFluidState(pos).isSource() && level.getFluidState(pos).is(Fluids.WATER))
            return PipeFluid.WATER;
        if (level.getFluidState(pos).isSource() && level.getFluidState(pos).is(Fluids.LAVA))
            return PipeFluid.LAVA;
        BlockState bs = level.getBlockState(pos);
        if ((bs.is(Blocks.CAMPFIRE) || bs.is(Blocks.SOUL_CAMPFIRE)) && bs.getValue(BlockStateProperties.LIT))
            return PipeFluid.SMOKE;
        return PipeFluid.NONE;
    }

    static PipeFluid getEntityFluid(BlockEntity be) {
        if (be instanceof CopperPipeBlockEntity p) return p.pipeFluid;
        return PipeFluid.NONE;
    }

    static boolean isFluidPipe(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CopperPipeBlockEntity;
    }

    // -------------------------------------------------------------------------
    // Item transport
    // -------------------------------------------------------------------------

    private boolean pushItem(Level level, BlockPos pos, Direction outputDir) {
        ItemStack stack = itemHandler.getStackInSlot(0);
        if (stack.isEmpty()) return false;

        BlockPos outputPos = pos.relative(outputDir);
        BlockState outputState = level.getBlockState(outputPos);

        if (outputState.isAir() || outputState.getFluidState().is(Fluids.WATER)) {
            ItemStack ejected = itemHandler.extractItem(0, 1, false);
            Vec3 center = Vec3.atCenterOf(pos);
            Vec3 velocity = Vec3.atLowerCornerOf(outputDir.getNormal()).scale(0.2);
            ItemEntity ie = new ItemEntity(level,
                    center.x + velocity.x, center.y + velocity.y, center.z + velocity.z,
                    ejected, velocity.x, velocity.y, velocity.z);
            ie.setPickUpDelay(10);
            level.addFreshEntity(ie);
            return true;
        }

        IItemHandler outputHandler = level.getCapability(
                Capabilities.ItemHandler.BLOCK, outputPos, outputDir.getOpposite());
        if (outputHandler != null) {
            ItemStack toInsert = itemHandler.extractItem(0, 1, true);
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(outputHandler, toInsert, false);
            if (remainder.isEmpty()) {
                itemHandler.extractItem(0, 1, false);
                return true;
            }
        }

        return false;
    }

    private boolean pullItem(Level level, BlockPos pos, BlockState state, Direction outputDir) {
        boolean locked = state.getValue(CopperPipeBlock.LOCKED);
        for (Direction dir : Direction.values()) {
            if (dir == outputDir) continue;
            // Fitted pipes only pull from faces that have an active connection arm.
            // This handles bent pipes correctly: the arm properties record the actual
            // configured input face(s), which may not be opposite the output.
            if (locked && !state.getValue(CopperPipeBlock.dirProp(dir))) continue;
            BlockPos neighborPos = pos.relative(dir);
            if (level.getBlockEntity(neighborPos) instanceof CopperPipeBlockEntity) continue;

            IItemHandler inputHandler = level.getCapability(
                    Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
            if (inputHandler == null) continue;

            for (int slot = 0; slot < inputHandler.getSlots(); slot++) {
                ItemStack candidate = inputHandler.extractItem(slot, 1, true);
                if (!candidate.isEmpty()) {
                    ItemStack remainder = itemHandler.insertItem(0, candidate, false);
                    if (remainder.isEmpty()) {
                        inputHandler.extractItem(slot, 1, false);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("transferCooldown", transferCooldown);
        tag.putString("pipeFluid", pipeFluid.name());
        tag.putInt("vibrationCooldown", vibrationCooldown);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        transferCooldown = tag.getInt("transferCooldown");
        if (tag.contains("pipeFluid")) {
            try { pipeFluid = PipeFluid.valueOf(tag.getString("pipeFluid")); }
            catch (IllegalArgumentException ignored) { pipeFluid = PipeFluid.NONE; }
        }
        vibrationCooldown = tag.getInt("vibrationCooldown");
    }
}
