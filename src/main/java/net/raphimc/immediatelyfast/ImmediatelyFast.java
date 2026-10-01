package net.raphimc.immediatelyfast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ImmediatelyFast {

    public static final Logger LOGGER          = LoggerFactory.getLogger("ImmediatelyFast");
    public static final int    FONT_ATLAS_SIZE = 1024;
    public static final int    MAP_ATLAS_SIZE  = 2048;

    public static boolean fontAtlasResizing  = true;
    public static boolean mapAtlasGeneration = true;

    private ImmediatelyFast() {
    }
}
