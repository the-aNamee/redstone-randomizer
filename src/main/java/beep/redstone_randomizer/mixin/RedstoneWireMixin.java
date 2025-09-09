package beep.redstone_randomizer.mixin;

import beep.redstone_randomizer.ModBlocks;
import beep.redstone_randomizer.RandomizerBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RedstoneWireBlock.class)
public class RedstoneWireMixin {
	@Inject(at = @At("HEAD"), method = "connectsTo*", cancellable = true)
    private static void connectsTo(BlockState state, @Nullable Direction dir, CallbackInfoReturnable<Boolean> cir) {
        if (state.isOf(ModBlocks.RANDOMIZER)) {
            Direction direction = state.get(RandomizerBlock.FACING);
            cir.setReturnValue(direction == dir || direction.getOpposite() == dir);
            cir.cancel();
        }
	}
}
