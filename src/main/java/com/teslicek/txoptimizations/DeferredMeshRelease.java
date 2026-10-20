package com.teslicek.txoptimizations;

import net.minecraft.client.renderer.chunk.SectionMesh;

public interface DeferredMeshRelease {

    void txoptimizations$releaseLater(SectionMesh mesh);
}
