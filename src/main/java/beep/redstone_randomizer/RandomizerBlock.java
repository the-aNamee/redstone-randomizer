package beep.redstone_randomizer;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

public class RandomizerBlock extends HorizontalFacingBlock {
    public static final MapCodec<RandomizerBlock> CODEC = createCodec(RandomizerBlock::new);
    public static final IntProperty RANDOM_FACTOR;
    public static final BooleanProperty POWERED;
    public static final BooleanProperty PPOWERED;
    private static final VoxelShape SHAPE;

    public MapCodec<RandomizerBlock> getCodec() {
        return CODEC;
    }

    public RandomizerBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(RANDOM_FACTOR, 0).with(POWERED, false).with(PPOWERED, false)); // Set the default BlockState.
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return state.get(RANDOM_FACTOR);
    }

    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        return !world.isClient() && direction.getAxis() != state.get(FACING).getAxis() ? state : super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random); // ChatGPT says that this is just making sure that the update is coming from the input axis.
    }

    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(POWERED)) {
            Direction direction = state.get(FACING);
            double d = (double)pos.getX() + (double)0.5F + (random.nextDouble() - (double)0.5F) * 0.2;
            double e = (double)pos.getY() + 0.4 + (random.nextDouble() - (double)0.5F) * 0.2;
            double f = (double)pos.getZ() + (double)0.5F + (random.nextDouble() - (double)0.5F) * 0.2;
            float g = -5.0F;
            if (random.nextBoolean()) {
                g = (float)state.get(RANDOM_FACTOR) * 2 - 1;
            }

            g /= 16.0F;
            double h = (g * (float)direction.getOffsetX());
            double i = (g * (float)direction.getOffsetZ());
            world.addParticleClient(DustParticleEffect.DEFAULT, d + h, e, f + i, 0.0F, 0.0F, 0.0F);
        }
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, RANDOM_FACTOR, POWERED, PPOWERED});
    }

    @Override
    // Set the shape to be a 2 pixel tall slab.
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    // Make it so that it gets placed in the direction the player is facing.
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite()).with(RANDOM_FACTOR, Random.create().nextBetween(0, 15));
    }

    // Tell MC that redstone should be redirected into this block.
    @Override
    protected boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    // Emit strong redstone power.
    @Override
    protected int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        if (direction == state.get(FACING) && state.get(POWERED) && state.get(RANDOM_FACTOR) > 7) {
            return 15;
        } {
            return 0;
        }
    }

    // Emit weak redstone power. This is just the same as the strong power.
    @Override
    protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return this.getStrongRedstonePower(state, world, pos, direction);
    }

    // When a neighbor updates check if the redstone state has changed.
    // This is so weird and I don't know if it is my fault or mojank, but it seems to work.
    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        // Make sure that we are allowed to exist here.
        if (!canPlaceAt(state, world, pos)) {
            BlockEntity blockEntity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;
            dropStacks(state, world, pos, blockEntity);
            world.removeBlock(pos, false);

            for(Direction direction : Direction.values()) {
                world.updateNeighbors(pos.offset(direction), this);
            }

            return;
        }


        Direction checking_dir = state.get(FACING);
        BlockPos checking_pos = pos.add(checking_dir.getVector());
        int power = world.getEmittedRedstonePower(checking_pos, checking_dir); // Get the redstone power from behind us.

        // Check if power is more than zero.
        boolean is_powered_now = power > 0;

        // If the state should change then we do that.
        if (is_powered_now != state.get(PPOWERED)) {
            world.setBlockState(pos, state.with(PPOWERED, is_powered_now), 3);
            world.scheduleBlockTick(pos, this, 2); // We set it two ticks (one redstone tick) after receiving power.
        }
    }

    // Actually set the state and do the random thing.
    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        boolean is_now_powered = state.get(PPOWERED);
        BlockState new_state = state.with(POWERED, is_now_powered);

        if (is_now_powered) {
            new_state = new_state.with(RANDOM_FACTOR, random.nextBetween(0, 15));
        }

        world.setBlockState(pos, new_state, 3);

        RedstoneRandomizer.LOGGER.info("Scheduled: {}", state.get(PPOWERED));
    }

    // Checks if this block can be placed above another block.


    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return world.getBlockState(pos.down()).isSideSolid(world, pos.down(), Direction.UP, SideShapeType.RIGID);
    }

    static {
        RANDOM_FACTOR = IntProperty.of("random_factor", 0, 15);
        POWERED = BooleanProperty.of("powered");
        PPOWERED = BooleanProperty.of("ppowered");
        SHAPE = Block.createColumnShape(16.0F, 0.0F, 2.0F);
    }
}
