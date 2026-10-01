package fr.madu59.obe.client.compat;

import java.util.Arrays;
import java.util.List;

import fr.madu59.obe.client.platform.PlatformHelper;

public class ModCompat {
    private static final List<String> incompatibleMods = Arrays.asList("optifine","embeddium","optifabric");

    public static boolean isIncompatibilityDetected(){
        for(String mod : incompatibleMods){
            if(PlatformHelper.isModLoaded(mod)) return true;
        }
        return false;
    }
}
