package com.teslicek.txoptimizations.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.SectionTaskDynamicQueue;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = SectionTaskDynamicQueue.class, priority = 999)
public abstract class SectionTaskDynamicQueueInboxMixin {

    @Unique
    private static final int RECOMPILE_QUOTA = 2;

    @Shadow
    private int recompileQuota;

    @Shadow
    @Final
    private List<SectionRenderDispatcher.RenderSection.SectionTask> tasks;

    @Unique
    private final ConcurrentLinkedQueue<SectionRenderDispatcher.RenderSection.SectionTask> txoptimizations$inbox = new ConcurrentLinkedQueue<>();

    @Unique
    private final AtomicInteger txoptimizations$inboxSize = new AtomicInteger();

    @Overwrite
    public void add(SectionRenderDispatcher.RenderSection.SectionTask task) {
        this.txoptimizations$inbox.add(task);
        this.txoptimizations$inboxSize.incrementAndGet();
    }

    @Overwrite
    public synchronized SectionRenderDispatcher.RenderSection.@Nullable SectionTask poll(Vec3 cameraPos) {
        this.txoptimizations$drainInbox();

        ObjectArrayList<SectionRenderDispatcher.RenderSection.SectionTask> list = (ObjectArrayList<SectionRenderDispatcher.RenderSection.SectionTask>) this.tasks;
        Object[] elements              = list.elements();
        int      size                  = list.size();
        int      kept                  = 0;
        int      bestInitial           = -1;
        int      bestRecompile         = -1;
        double   bestInitialDistance   = Double.MAX_VALUE;
        double   bestRecompileDistance = Double.MAX_VALUE;

        for (int index = 0; index < size; index ++) {
            SectionRenderDispatcher.RenderSection.SectionTask task = (SectionRenderDispatcher.RenderSection.SectionTask) elements[index];

            if (task.isCancelled.get())
                continue;

            double distance = task.getRenderOrigin().distToCenterSqr(cameraPos);

            elements[kept] = task;

            if (!task.isRecompile() && distance < bestInitialDistance) {
                bestInitialDistance = distance;
                bestInitial         = kept;
            }

            if (task.isRecompile() && distance < bestRecompileDistance) {
                bestRecompileDistance = distance;
                bestRecompile         = kept;
            }

            kept ++;
        }

        list.size(kept);

        boolean hasRecompile = bestRecompile >= 0;
        boolean hasInitial   = bestInitial >= 0;

        if (!hasRecompile || hasInitial && (this.recompileQuota <= 0 || !(bestRecompileDistance < bestInitialDistance))) {
            this.recompileQuota = RECOMPILE_QUOTA;

            return bestInitial >= 0 ? list.remove(bestInitial) : null;
        }

        this.recompileQuota --;

        return list.remove(bestRecompile);
    }

    @Overwrite
    public int size() {
        return this.tasks.size() + this.txoptimizations$inboxSize.get();
    }

    @Overwrite
    public synchronized void clear() {
        this.txoptimizations$drainInbox();

        for (SectionRenderDispatcher.RenderSection.SectionTask task : this.tasks)
            task.cancel();

        this.tasks.clear();
    }

    @Unique
    private void txoptimizations$drainInbox() {
        SectionRenderDispatcher.RenderSection.SectionTask task;

        while ((task = this.txoptimizations$inbox.poll()) != null) {
            this.txoptimizations$inboxSize.decrementAndGet();
            this.tasks.add(task);
        }
    }
}
