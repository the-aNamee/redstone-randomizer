package beep.redstone_randomizer;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedstoneRandomizer implements ModInitializer {
	public static final String MOD_ID = "redstone-randomizer";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Redstone Randomizer mod.");
        ModBlocks.init();
	}
}
