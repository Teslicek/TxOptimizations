package com.teslicek.txoptimizations;

import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import org.jspecify.annotations.Nullable;

public final class ModelCopies {

    private static final String                  MINECRAFT     = "net.minecraft.";
    private static final Set<Class<?>>           SHAREABLE     = Set.of(ModelPart.class, ModelPart[].class, List.class, KeyframeAnimation.class, Function.class, String.class);
    private static final Layout                  UNSUPPORTED   = new Layout(false, new Field[0]);
    private static final Map<Source, List<Pair>> SOURCES       = new EnumMap<>(Source.class);

    private static final ClassValue<Field[]> FIELDS = new ClassValue<>() {
        @Override
        protected Field[] computeValue(Class<?> type) {
            return walkedFields(type);
        }
    };

    private static final ClassValue<Layout> LAYOUTS = new ClassValue<>() {
        @Override
        protected Layout computeValue(Class<?> type) {
            return layout(type);
        }
    };

    private static Map<Model<?>, Model<?>>   twins     = Map.of();
    private static Map<Model<?>, Model<?>>   originals = Map.of();
    private static Map<ModelPart, ModelPart> parts     = Map.of();

    private ModelCopies() {
    }

    public static void replace(Source source, Object originals, Object copies) {
        RenderSystem.assertOnRenderThread();
        SOURCES.put(source, new Walk().pair(originals, copies));

        Map<Model<?>, Model<?>>   byOriginal = new IdentityHashMap<>();
        Map<Model<?>, Model<?>>   byCopy     = new IdentityHashMap<>();
        Map<ModelPart, ModelPart> byPart     = new IdentityHashMap<>();

        for (List<Pair> pairs : SOURCES.values()) {
            for (Pair pair : pairs) {
                if (byOriginal.put(pair.original, pair.copy) != null)
                    throw new IllegalStateException("Model " + describe(pair.original) + " was copied twice");

                byCopy.put(pair.copy, pair.original);

                for (int index = 0; index < pair.originals.length; index ++) {
                    if (byPart.put(pair.copies[index], pair.originals[index]) != null)
                        throw new IllegalStateException("A part of " + describe(pair.original) + " belongs to two copied models");
                }
            }
        }

        twins     = byOriginal;
        originals = byCopy;
        parts     = byPart;
    }

    public static <M extends Model<?>> M mainModel(M model) {
        if (!RenderThreadStack.isMain())
            return model;

        @SuppressWarnings("unchecked")
        M twin = (M) twins.get(model);

        if (twin != null)
            return twin;

        if (originals.containsKey(model))
            return model;

        RenderWorker.awaitFromMain();

        return model;
    }

    public static Model<?> submittedModel(Model<?> model) {
        if (!RenderThreadStack.isMain())
            return model;

        Model<?> original = originals.get(model);

        if (original != null)
            return original;

        if (model.getClass() != Model.Simple.class)
            return model;

        ModelPart part = parts.get(model.root());

        return part == null ? model : new Model.Simple(part, model.renderType());
    }

    private static Field[] walkedFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();

        for (Class<?> level = type; level != null && level.getName().startsWith(MINECRAFT); level = level.getSuperclass()) {
            for (Field field : level.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive())
                    fields.add(accessible(field));
            }
        }

        return fields.toArray(Field[]::new);
    }

    private static Layout layout(Class<?> type) {
        List<Field> references = new ArrayList<>();

        for (Class<?> level = type; level != Model.class; level = level.getSuperclass()) {
            for (Field field : level.getDeclaredFields()) {
                int      modifiers = field.getModifiers();
                Class<?> kind      = field.getType();

                if (Modifier.isStatic(modifiers))
                    continue;

                if (kind.isPrimitive())
                    continue;

                if (!Modifier.isFinal(modifiers) || (!kind.isEnum() && !SHAREABLE.contains(kind)))
                    return UNSUPPORTED;

                references.add(accessible(field));
            }
        }

        return new Layout(true, references.toArray(Field[]::new));
    }

    private static boolean walked(Object value) {
        if (value instanceof ModelPart)
            return false;

        if (value instanceof Model<?> || value instanceof Object[] || value instanceof List<?> || value instanceof Map<?, ?>)
            return true;

        Class<?> type = value.getClass();

        return type.getName().startsWith(MINECRAFT) && !type.isHidden();
    }

    private static boolean within(@Nullable Object value, Set<ModelPart> tree) {
        if (value instanceof ModelPart part)
            return tree.contains(part);

        if (value instanceof ModelPart[] array)
            return tree.containsAll(Arrays.asList(array));

        if (value instanceof List<?> list)
            return tree.containsAll(list);

        return true;
    }

    private static void pairParts(ModelPart original, ModelPart copy, List<ModelPart> originals, List<ModelPart> copies, String path) {
        if (original == copy)
            throw new IllegalStateException(path + " shares a model part with its copy");

        if (original.cubes.size() != copy.cubes.size() || !original.children.keySet().equals(copy.children.keySet()))
            throw new IllegalStateException(path + " is built differently in its copy");

        for (int index = 0; index < original.cubes.size(); index ++) {
            if (original.cubes.get(index) == copy.cubes.get(index))
                throw new IllegalStateException(path + " shares a model cube with its copy");
        }

        originals.add(original);
        copies.add(copy);

        for (Map.Entry<String, ModelPart> child : original.children.entrySet())
            pairParts(child.getValue(), copy.children.get(child.getKey()), originals, copies, path + "/" + child.getKey());
    }

    private static Field accessible(Field field) {
        field.setAccessible(true);

        return field;
    }

    private static @Nullable Object read(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot read " + field + " of " + describe(owner), exception);
        }
    }

    private static String describe(Object object) {
        return object.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(object));
    }

    public enum Source {
        ENTITIES,
        BLOCK_ENTITIES
    }

    private record Step(@Nullable Object original, @Nullable Object copy, String path) {
    }

    private record Layout(boolean supported, Field[] references) {

        private boolean holdsOnly(Model<?> model, Set<ModelPart> tree) {
            for (Field field : this.references) {
                if (!within(read(field, model), tree))
                    return false;
            }

            return true;
        }
    }

    private static final class Pair {

        private final Model<?>    original;
        private final Model<?>    copy;
        private final ModelPart[] originals;
        private final ModelPart[] copies;

        private Pair(Model<?> original, Model<?> copy, ModelPart[] originals, ModelPart[] copies) {
            this.original  = original;
            this.copy      = copy;
            this.originals = originals;
            this.copies    = copies;
        }

    }

    private static final class Walk {

        private final Map<Object, Object> forward  = new IdentityHashMap<>();
        private final Map<Object, Object> backward = new IdentityHashMap<>();
        private final ArrayDeque<Step>    pending  = new ArrayDeque<>();
        private final List<Pair>          pairs    = new ArrayList<>();

        private List<Pair> pair(Object originals, Object copies) {
            this.pending.push(new Step(originals, copies, "renderers"));

            while (!this.pending.isEmpty())
                this.visit(this.pending.pop());

            return this.pairs;
        }

        private void visit(Step step) {
            Object original = step.original();
            Object copy     = step.copy();

            if (original == null || copy == null) {
                if (original != copy)
                    throw new IllegalStateException("Copied renderers differ at " + step.path() + ": " + (original == null ? "null" : describe(original)) + " against " + (copy == null ? "null" : describe(copy)));

                return;
            }

            Class<?> type = original.getClass();

            if (!walked(original) || !this.link(original, copy, step.path()) || original == copy)
                return;

            if (type != copy.getClass())
                throw new IllegalStateException("Copied renderers differ at " + step.path() + ": " + type.getName() + " against " + copy.getClass().getName());

            if (original instanceof Model<?> model) {
                if (type.getName().startsWith(MINECRAFT))
                    this.pairModel(model, (Model<?>) copy, step.path());

                return;
            }

            if (original instanceof Object[] array) {
                this.pushAll(array, (Object[]) copy, step.path());

                return;
            }

            if (original instanceof List<?> list) {
                this.pushAll(list.toArray(), ((List<?>) copy).toArray(), step.path());

                return;
            }

            if (original instanceof Map<?, ?> map) {
                this.pushValues(map, (Map<?, ?>) copy, step.path());

                return;
            }

            for (Field field : FIELDS.get(type))
                this.pending.push(new Step(read(field, original), read(field, copy), step.path() + "." + field.getName()));
        }

        private boolean link(Object original, Object copy, String path) {
            Object known = this.forward.get(original);

            if (known != null) {
                if (known != copy)
                    throw new IllegalStateException(path + " reaches " + describe(original) + " paired with another copy");

                return false;
            }

            if (this.backward.containsKey(copy) || original != copy && (this.forward.containsKey(copy) || this.backward.containsKey(original)))
                throw new IllegalStateException(path + " pairs " + describe(original) + " with " + describe(copy) + " inconsistently");

            this.forward.put(original, copy);
            this.backward.put(copy, original);

            return true;
        }

        private void pushAll(Object[] originals, Object[] copies, String path) {
            if (originals.length != copies.length)
                throw new IllegalStateException("Copied renderers differ at " + path + ": " + originals.length + " elements against " + copies.length);

            for (int index = 0; index < originals.length; index ++)
                this.pending.push(new Step(originals[index], copies[index], path + "[" + index + "]"));
        }

        private void pushValues(Map<?, ?> originals, Map<?, ?> copies, String path) {
            if (originals.size() != copies.size())
                return;

            for (Object key : originals.keySet()) {
                if (!copies.containsKey(key))
                    return;
            }

            int index = 0;

            for (Map.Entry<?, ?> entry : originals.entrySet()) {
                this.pending.push(new Step(entry.getValue(), copies.get(entry.getKey()), path + "{" + index + "}"));
                index ++;
            }
        }

        private void pairModel(Model<?> original, Model<?> copy, String path) {
            List<ModelPart> originals = new ArrayList<>();
            List<ModelPart> copies    = new ArrayList<>();

            pairParts(original.root(), copy.root(), originals, copies, path);

            Layout layout = LAYOUTS.get(original.getClass());

            if (!layout.supported())
                return;

            Object originalClaimant = original.getClass() == Model.Simple.class ? null : original;
            Object copyClaimant     = original.getClass() == Model.Simple.class ? null : copy;

            for (int index = 0; index < originals.size(); index ++) {
                if (((ModelPartOwner) (Object) originals.get(index)).txoptimizations$owner() != originalClaimant || ((ModelPartOwner) (Object) copies.get(index)).txoptimizations$owner() != copyClaimant)
                    return;
            }

            Set<ModelPart> tree = Collections.newSetFromMap(new IdentityHashMap<>());

            tree.addAll(originals);

            if (!layout.holdsOnly(original, tree))
                return;

            this.pairs.add(new Pair(original, copy, originals.toArray(ModelPart[]::new), copies.toArray(ModelPart[]::new)));
        }
    }
}
