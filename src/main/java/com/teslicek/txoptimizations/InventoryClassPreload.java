package com.teslicek.txoptimizations;

import java.util.List;
import net.minecraft.client.Minecraft;

public final class InventoryClassPreload {

    private static final List<String> CLASSES = List.of(
        "net.minecraft.client.gui.ItemSlotMouseAction",
        "net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen",
        "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen",
        "net.minecraft.client.gui.screens.recipebook.RecipeBookComponent",
        "net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent",
        "net.minecraft.client.gui.screens.recipebook.RecipeBookComponent$TabInfo",
        "net.minecraft.world.entity.player.StackedItemContents",
        "net.minecraft.world.entity.player.StackedContents",
        "net.minecraft.client.gui.screens.recipebook.SlotSelectTime",
        "net.minecraft.client.gui.screens.recipebook.GhostSlots",
        "net.minecraft.client.gui.screens.recipebook.RecipeBookPage",
        "net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent",
        "net.minecraft.client.gui.screens.recipebook.RecipeButton",
        "net.minecraft.client.gui.screens.inventory.EffectsInInventory",
        "net.minecraft.client.gui.BundleMouseActions",
        "net.minecraft.stats.RecipeBookSettings$1",
        "net.minecraft.client.gui.screens.recipebook.GhostSlots$GhostSlot",
        "net.minecraft.client.renderer.entity.state.ArmedEntityRenderState$1",
        "net.minecraft.client.model.AnimationUtils",
        "net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRenderEvents",
        "net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRenderEvents$AllowCapeRender"
    );

    private InventoryClassPreload() {
    }

    public static void start() {
        ClassLoader loader = Minecraft.class.getClassLoader();

        Thread.ofPlatform().daemon().name("TxOptimizations Inventory Preload").start(() -> {
            for (String name : CLASSES) {
                try {
                    Class.forName(name, false, loader);
                } catch (ClassNotFoundException exception) {
                    throw new IllegalStateException("Inventory class to preload is missing: " + name, exception);
                }
            }
        });
    }
}
