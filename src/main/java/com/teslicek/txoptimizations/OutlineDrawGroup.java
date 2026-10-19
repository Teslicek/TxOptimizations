package com.teslicek.txoptimizations;

import java.util.List;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;

public interface OutlineDrawGroup {

    List<StagedVertexBuffer.Draw> txoptimizations$draws();

    List<PreparedRenderType> txoptimizations$drawRenderTypes();
}
