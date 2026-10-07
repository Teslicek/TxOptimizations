package com.teslicek.txoptimizations.mixin;

import com.mojang.renderpearl.frontend.FrontendRenderPipeline;
import com.teslicek.txoptimizations.PipelineUniforms;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FrontendRenderPipeline.class)
public abstract class FrontendRenderPipelineUniformsMixin implements PipelineUniforms {

    @Unique
    private static final int NO_SLOT = -1;

    @Shadow
    @Final
    private Object2IntMap<String> uniformIndices;

    @Unique
    private String[] txoptimizations$names;

    @Unique
    private int[] txoptimizations$slots;

    @Unique
    private Object2IntMap<String> txoptimizations$slotsByName;

    @Override
    public String[] txoptimizations$uniformNames() {
        if (this.txoptimizations$names == null)
            this.txoptimizations$flatten();

        return this.txoptimizations$names;
    }

    @Override
    public int[] txoptimizations$uniformSlots() {
        if (this.txoptimizations$slots == null)
            this.txoptimizations$flatten();

        return this.txoptimizations$slots;
    }

    @Override
    public int txoptimizations$uniformSlot(String name) {
        Object2IntMap<String> slots = this.txoptimizations$slotsByName;

        if (slots == null) {
            slots                            = ((UnmodifiableObject2IntMapAccessor) this.uniformIndices).txoptimizations$map();
            this.txoptimizations$slotsByName = slots;
        }

        return slots.getOrDefault(name, NO_SLOT);
    }

    @Unique
    private void txoptimizations$flatten() {
        Object2IntMap<String> map   = ((UnmodifiableObject2IntMapAccessor) this.uniformIndices).txoptimizations$map();
        String[]              names = new String[map.size()];
        int[]                 slots = new int[map.size()];
        int                   index = 0;

        for (Object2IntMap.Entry<String> uniform : Object2IntMaps.fastIterable(map)) {
            names[index] = uniform.getKey();
            slots[index] = uniform.getIntValue();
            index ++;
        }

        this.txoptimizations$slots = slots;
        this.txoptimizations$names = names;
    }
}
