package com.teslicek.txoptimizations;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class GuiItemCache {

    private static final long                  UNUSED_FRAMES = 200L;
    private static final int                   MAX_ENTRIES   = 4096;
    private static final Map<ItemStack, Entry> ENTRIES       = new IdentityHashMap<>();

    private static @Nullable Entry pending;
    private static boolean         hit;
    private static long            lastEviction;

    private GuiItemCache() {
    }

    public static TrackingItemStackRenderState state(@Nullable LivingEntity owner, @Nullable Level level, ItemStack stack, int seed) {
        if (pending != null)
            throw new IllegalStateException("A GUI item state was requested before the previous one was resolved");

        long   frame   = ClientClock.frame();
        Object models  = Minecraft.getInstance().getModelManager().bakedItemStackModels;
        int    version = ((ComponentVersion) stack.getComponents()).txoptimizations$version();
        Entry  entry   = ENTRIES.get(stack);

        evictUnused(frame);

        if (entry != null && entry.version == version && entry.count == stack.getCount() && entry.owner == owner && entry.level == level && entry.seed == seed && entry.models == models) {
            entry.frame = frame;
            pending     = entry;
            hit         = true;

            return entry.state;
        }

        entry   = new Entry(new TrackingItemStackRenderState(), version, stack.getCount(), owner, level, seed, models, frame);
        pending = entry;
        hit     = false;

        return entry.state;
    }

    public static void resolve(ItemModelResolver resolver, TrackingItemStackRenderState state, ItemStack stack, ItemDisplayContext context, @Nullable Level level, @Nullable ItemOwner owner, int seed) {
        Entry entry = pending;

        if (entry == null || entry.state != state)
            throw new IllegalStateException("A GUI item was resolved without the state the cache handed out");

        pending = null;

        if (hit)
            return;

        ItemModelPurity.begin();

        boolean pure = false;

        try {
            resolver.updateForTopItem(state, stack, context, level, owner, seed);
        } finally {
            pure = ItemModelPurity.end();
        }

        if (!pure) {
            ENTRIES.remove(stack);
            return;
        }

        if (ENTRIES.size() >= MAX_ENTRIES)
            ENTRIES.clear();

        ENTRIES.put(stack, entry);
    }

    private static void evictUnused(long frame) {
        if (frame - lastEviction < UNUSED_FRAMES)
            return;

        lastEviction = frame;

        for (Iterator<Entry> iterator = ENTRIES.values().iterator(); iterator.hasNext(); ) {
            if (frame - iterator.next().frame >= UNUSED_FRAMES)
                iterator.remove();
        }
    }

    private static final class Entry {

        private final TrackingItemStackRenderState state;
        private final int                          version;
        private final int                          count;
        private final @Nullable LivingEntity       owner;
        private final @Nullable Level              level;
        private final int                          seed;
        private final Object                       models;
        private long                               frame;

        private Entry(TrackingItemStackRenderState state, int version, int count, @Nullable LivingEntity owner, @Nullable Level level, int seed, Object models, long frame) {
            this.state   = state;
            this.version = version;
            this.count   = count;
            this.owner   = owner;
            this.level   = level;
            this.seed    = seed;
            this.models  = models;
            this.frame   = frame;
        }
    }
}
