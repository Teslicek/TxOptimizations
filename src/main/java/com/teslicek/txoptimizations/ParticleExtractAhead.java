package com.teslicek.txoptimizations;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;

public interface ParticleExtractAhead {

    void txoptimizations$extractAhead(Frustum frustum, Camera camera, float partialTickTime);
}
