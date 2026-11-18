package com.teslicek.txoptimizations;

import java.util.List;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.jspecify.annotations.Nullable;

public record ItemShapes(List<BakedQuad> quads, int generation, int @Nullable [] shapes) {

    public static ItemShapes of(List<BakedQuad> quads) {
        ItemShapesSlot slot = (ItemShapesSlot) (Object) quads.getFirst();
        ItemShapes     memo = slot.txoptimizations$itemShapes();

        if (memo != null && memo.quads == quads && memo.generation == GpuCubes.generation())
            return memo;

        memo = new ItemShapes(quads, GpuCubes.generation(), read(quads));
        slot.txoptimizations$setItemShapes(memo);

        return memo;
    }

    private static int @Nullable [] read(List<BakedQuad> quads) {
        BakedQuad.MaterialInfo material = quads.getFirst().materialInfo();
        int[]                  shapes   = new int[quads.size()];

        for (int index = 0; index < shapes.length; index ++) {
            BakedQuad quad = quads.get(index);

            if (quad.materialInfo() != material)
                return null;

            shapes[index] = ((GpuCube) (Object) quad).txoptimizations$gpuIndex();
        }

        return shapes;
    }
}
