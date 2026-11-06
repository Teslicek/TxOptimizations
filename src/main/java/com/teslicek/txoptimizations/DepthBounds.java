package com.teslicek.txoptimizations;

import com.mojang.renderpearl.backend.vulkan.VulkanFeatureSets;
import com.mojang.renderpearl.backend.vulkan.init.FeatureSet;
import com.mojang.renderpearl.backend.vulkan.init.VulkanFeature;
import java.util.Set;

public final class DepthBounds {

    public static final FeatureSet FEATURES = new FeatureSet("TxOptimizations depth bounds", Set.of(), Set.of(new VulkanFeature(VulkanFeatureSets.VK10_FEATURES_STRUCT, "depthBounds")));
    public static final float      FAR      = 0.0F;

    private static final Set<String> FAR_ONLY = Set.of("txoptimizations:pipeline/end_sky_far", "txoptimizations:pipeline/end_flash_far");

    private DepthBounds() {
    }

    public static void deviceCreated(FeatureSet enabledFeatures) {
        if (!enabledFeatures.contains(FEATURES))
            throw new IllegalStateException("TxOptimizations needs the Vulkan depthBounds device feature, which this GPU did not enable");
    }

    public static boolean farOnly(String pipeline) {
        return FAR_ONLY.contains(pipeline);
    }
}
