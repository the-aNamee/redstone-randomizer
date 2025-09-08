package beep.redstone_randomizer;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

public class RandomizerBlock extends AbstractRedstoneGateBlock {
    public static final MapCodec<RandomizerBlock> CODEC = createCodec(RandomizerBlock::new);
    public static final IntProperty RANDOM_FACTOR;

    public MapCodec<RandomizerBlock> getCodec() {
        return CODEC;
    }

    public RandomizerBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateManager.getDefaultState()).with(FACING, Direction.NORTH)).with(RANDOM_FACTOR, 0))).with(POWERED, false)); // Set the default blockstate.
    }

    protected int getUpdateDelayInternal(BlockState state) {
        return 2; // We have a two tick delay. Which is one redstone tick.
    }

    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        if (direction == Direction.DOWN && !this.canPlaceAbove(world, neighborPos, neighborState)) {
            return Blocks.AIR.getDefaultState(); // This checks if we aren't on top of a good block and turns us to air if we aren't.
        } else {
            return !world.isClient() && direction.getAxis() != ((Direction)state.get(FACING)).getAxis() ? (BlockState)state : super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random); // ChatGPT says that this is just making sure that the update is coming from the input axis.
        }
    }

    public boolean isLocked(WorldView world, BlockPos pos, BlockState state) {
        return this.getMaxInputLevelSides(world, pos, state) > 0;
    }

    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if ((Boolean)state.get(POWERED)) {
            Direction direction = (Direction)state.get(FACING);
            double d = (double)pos.getX() + (double)0.5F + (random.nextDouble() - (double)0.5F) * 0.2;
            double e = (double)pos.getY() + 0.4 + (random.nextDouble() - (double)0.5F) * 0.2;
            double f = (double)pos.getZ() + (double)0.5F + (random.nextDouble() - (double)0.5F) * 0.2;
            float g = -5.0F;
            if (random.nextBoolean()) {
                g = (float)((Integer)state.get(RANDOM_FACTOR) * 2 - 1);
            }

            g /= 16.0F;
            double h = (double)(g * (float)direction.getOffsetX());
            double i = (double)(g * (float)direction.getOffsetZ());
            world.addParticleClient(DustParticleEffect.DEFAULT, d + h, e, f + i, (double)0.0F, (double)0.0F, (double)0.0F);
        }
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, RANDOM_FACTOR, POWERED});
    }

    @Override
    protected void updatePowered(World world, BlockPos pos, BlockState state) {
        Boolean is_powered = world.getBlockState(pos).get(POWERED);
        RedstoneRandomizer.LOGGER.info("Update powered: " + is_powered);
        super.updatePowered(world, pos, state);
    }

    static {
        RANDOM_FACTOR = IntProperty.of("random_factor", 0, 15);
    }
}
