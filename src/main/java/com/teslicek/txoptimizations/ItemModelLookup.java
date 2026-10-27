package com.teslicek.txoptimizations;

import java.util.Map;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public final class ItemModelLookup {

    private ItemModelLookup() {
    }

    public static ItemModel model(ModelManager manager, Item item, Identifier modelId) {
        ItemModelSlot              slot   = (ItemModelSlot) item;
        Map<Identifier, ItemModel> models = manager.bakedItemStackModels;

        if (slot.txoptimizations$modelId() == modelId && slot.txoptimizations$modelMap() == models)
            return slot.txoptimizations$model();

        ItemModel found = manager.getItemModel(modelId);

        slot.txoptimizations$setModel(modelId, models, found);

        return found;
    }

    public static ClientItem.Properties properties(ModelManager manager, Item item, Identifier modelId) {
        ItemModelSlot                          slot       = (ItemModelSlot) item;
        Map<Identifier, ClientItem.Properties> properties = manager.itemProperties;

        if (slot.txoptimizations$propertiesId() == modelId && slot.txoptimizations$propertiesMap() == properties)
            return slot.txoptimizations$properties();

        ClientItem.Properties found = manager.getItemProperties(modelId);

        slot.txoptimizations$setProperties(modelId, properties, found);

        return found;
    }
}
