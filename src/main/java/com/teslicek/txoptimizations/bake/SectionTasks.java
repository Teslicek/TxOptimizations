package com.teslicek.txoptimizations.bake;

import java.util.Queue;
import org.jspecify.annotations.Nullable;

public interface SectionTasks {

    Queue<Runnable> txoptimizations$getSectionTasks();

    ChunkTasks.@Nullable Claim txoptimizations$getClaim();

    void txoptimizations$setClaim(ChunkTasks.@Nullable Claim claim);
}
