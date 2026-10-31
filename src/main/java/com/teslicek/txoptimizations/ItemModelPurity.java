package com.teslicek.txoptimizations;

import java.util.List;
import java.util.Set;
import net.minecraft.client.color.item.Constant;
import net.minecraft.client.color.item.CustomModelDataSource;
import net.minecraft.client.color.item.Dye;
import net.minecraft.client.color.item.Firework;
import net.minecraft.client.color.item.GrassColorSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.Potion;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.client.renderer.item.ConditionalItemModel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.EmptyModel;
import net.minecraft.client.renderer.item.MissingItemModel;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.properties.conditional.Broken;
import net.minecraft.client.renderer.item.properties.conditional.BundleHasSelectedItem;
import net.minecraft.client.renderer.item.properties.conditional.ComponentMatches;
import net.minecraft.client.renderer.item.properties.conditional.Damaged;
import net.minecraft.client.renderer.item.properties.conditional.HasComponent;
import net.minecraft.client.renderer.item.properties.numeric.BundleFullness;
import net.minecraft.client.renderer.item.properties.numeric.Count;
import net.minecraft.client.renderer.item.properties.numeric.Damage;
import net.minecraft.client.renderer.item.properties.select.Charge;
import net.minecraft.client.renderer.item.properties.select.ComponentContents;
import net.minecraft.client.renderer.item.properties.select.ContextDimension;
import net.minecraft.client.renderer.item.properties.select.ContextEntityType;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.client.renderer.item.properties.select.ItemBlockState;
import net.minecraft.client.renderer.item.properties.select.TrimMaterialProperty;

public final class ItemModelPurity {

    private static final Set<Class<?>> PURE_MODELS     = Set.of(CompositeModel.class, ConditionalItemModel.class, CuboidItemModelWrapper.class, EmptyModel.class, MissingItemModel.class, RangeSelectItemModel.class, SelectItemModel.class);
    private static final Set<Class<?>> PURE_PROPERTIES = Set.of(
        Broken.class,
        BundleHasSelectedItem.class,
        ComponentMatches.class,
        net.minecraft.client.renderer.item.properties.conditional.CustomModelDataProperty.class,
        Damaged.class,
        HasComponent.class,
        BundleFullness.class,
        Count.class,
        net.minecraft.client.renderer.item.properties.numeric.CustomModelDataProperty.class,
        Damage.class,
        Charge.class,
        ComponentContents.class,
        ContextDimension.class,
        ContextEntityType.class,
        net.minecraft.client.renderer.item.properties.select.CustomModelDataProperty.class,
        DisplayContext.class,
        ItemBlockState.class,
        TrimMaterialProperty.class
    );

    private static final Set<Class<?>> PURE_TINTS      = Set.of(Constant.class, CustomModelDataSource.class, Dye.class, Firework.class, GrassColorSource.class, Potion.class);

    private static boolean tracking;
    private static boolean impure;

    private ItemModelPurity() {
    }

    public static void begin() {
        if (tracking)
            throw new IllegalStateException("Item model purity is already being tracked");

        tracking = true;
        impure   = false;
    }

    public static boolean end() {
        if (!tracking)
            throw new IllegalStateException("Item model purity was not being tracked");

        tracking = false;

        return !impure;
    }

    public static void checkModel(Object model) {
        if (tracking && !PURE_MODELS.contains(model.getClass()))
            impure = true;
    }

    public static void checkProperty(Object property) {
        if (tracking && !PURE_PROPERTIES.contains(property.getClass()))
            impure = true;
    }

    public static void checkTints(List<ItemTintSource> tints) {
        if (!tracking)
            return;

        for (int index = 0; index < tints.size(); index ++) {
            if (!PURE_TINTS.contains(tints.get(index).getClass()))
                impure = true;
        }
    }

    public static void markImpure() {
        if (tracking)
            impure = true;
    }
}
