package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.BlockModelCopy;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockModelRenderState.class)
public abstract class BlockModelRenderStateCopyMixin implements BlockModelCopy {

    @Unique
    private static final Set<String> KNOWN_FIELDS = Set.of("modelParts", "transformation", "renderType", "specialRenderer", "specialRendererTransformation", "tintLayers", "blockLightCoords", "randomSource", "mesh", "renderStateData");

    @Unique
    private static final @Nullable VarHandle MESH = txoptimizations$field("mesh");

    @Unique
    private static final @Nullable VarHandle DATA = txoptimizations$field("renderStateData");

    @Unique
    private static final boolean COPYABLE = txoptimizations$knownFieldsOnly();

    @Shadow
    private @Nullable List<BlockStateModelPart> modelParts;

    @Shadow
    private @Nullable Matrix4fc transformation;

    @Shadow
    private @Nullable RenderType renderType;

    @Shadow
    private @Nullable SpecialModelRenderer<?> specialRenderer;

    @Shadow
    private @Nullable Matrix4fc specialRendererTransformation;

    @Shadow
    private @Nullable IntList tintLayers;

    @Shadow
    public int blockLightCoords;

    @Shadow
    public abstract void clear();

    @Shadow
    public abstract IntList tintLayers();

    @Override
    public boolean txoptimizations$copyable() {
        return COPYABLE;
    }

    @Override
    public boolean txoptimizations$hasAttachedData() {
        return DATA != null && DATA.get((BlockModelRenderState) (Object) this) instanceof Map<?, ?> data && !data.isEmpty();
    }

    @Override
    public void txoptimizations$copyFrom(BlockModelRenderState prepared) {
        BlockModelRenderStateCopyMixin source = (BlockModelRenderStateCopyMixin) (Object) prepared;

        this.clear();

        if (source.modelParts != null)
            this.modelParts = new ObjectArrayList<>(source.modelParts);

        if (MESH != null)
            MESH.set((BlockModelRenderState) (Object) this, MESH.get(prepared));

        this.transformation                = source.transformation;
        this.renderType                    = source.renderType;
        this.specialRenderer               = source.specialRenderer;
        this.specialRendererTransformation = source.specialRendererTransformation;
        this.blockLightCoords              = source.blockLightCoords;

        if (source.tintLayers != null && !source.tintLayers.isEmpty())
            this.tintLayers().addAll(source.tintLayers);
    }

    @Unique
    private static @Nullable VarHandle txoptimizations$field(String name) {
        Field field = Arrays.stream(BlockModelRenderState.class.getDeclaredFields()).filter(candidate -> candidate.getName().equals(name)).findFirst().orElse(null);

        if (field == null)
            return null;

        try {
            return MethodHandles.privateLookupIn(BlockModelRenderState.class, MethodHandles.lookup()).unreflectVarHandle(field);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot access " + name + " of block model render states", exception);
        }
    }

    @Unique
    private static boolean txoptimizations$knownFieldsOnly() {
        return Arrays.stream(BlockModelRenderState.class.getDeclaredFields()).filter(field -> !Modifier.isStatic(field.getModifiers())).allMatch(field -> KNOWN_FIELDS.contains(field.getName()));
    }
}
