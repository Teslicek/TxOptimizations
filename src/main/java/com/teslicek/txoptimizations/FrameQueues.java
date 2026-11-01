package com.teslicek.txoptimizations;

import java.util.List;
import net.minecraft.client.renderer.state.level.TransientBlockRenderState;
import net.minecraft.gizmos.SimpleGizmoCollector;

public final class FrameQueues {

    public static final FrameQueue<TransientBlock>                           TRANSIENT_BLOCKS   = new FrameQueue<>("Transient blocks");
    public static final FrameQueue<List<SimpleGizmoCollector.GizmoInstance>> MAIN_THREAD_GIZMOS = new FrameQueue<>("Main thread gizmos");

    private FrameQueues() {
    }

    public record TransientBlock(long blockNode, TransientBlockRenderState state) {
    }
}
