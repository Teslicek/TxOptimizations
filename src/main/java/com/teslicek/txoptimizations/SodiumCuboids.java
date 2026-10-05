package com.teslicek.txoptimizations;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import net.caffeinemc.mods.sodium.client.render.immediate.model.ModelCuboid;
import net.minecraft.client.model.geom.ModelPart;

public final class SodiumCuboids {

    private static final VarHandle CUBOID = findCuboid();

    private SodiumCuboids() {
    }

    public static ModelCuboid of(ModelPart.Cube cube) {
        ModelCuboid cuboid = (ModelCuboid) CUBOID.get(cube);

        if (cuboid == null)
            throw new IllegalStateException("Model cube has no Sodium cuboid");

        return cuboid;
    }

    private static VarHandle findCuboid() {
        try {
            return MethodHandles.privateLookupIn(ModelPart.Cube.class, MethodHandles.lookup()).findVarHandle(ModelPart.Cube.class, "sodium$cuboid", ModelCuboid.class);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Sodium cuboid field not found on ModelPart.Cube", exception);
        }
    }
}
