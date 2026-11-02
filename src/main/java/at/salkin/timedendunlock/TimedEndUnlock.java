package at.salkin.timedendunlock;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimedEndUnlock implements ModInitializer {
    public static final String MOD_ID = "timed_end_unlock";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Timed End Unlock");
    }
}

