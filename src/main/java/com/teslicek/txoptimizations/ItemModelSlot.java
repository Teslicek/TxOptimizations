package com.teslicek.txoptimizations;

import java.util.Map;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.resources.Identifier;

public interface ItemModelSlot {

    Identifier txoptimizations$modelId();

    Map<Identifier, ItemModel> txoptimizations$modelMap();

    ItemModel txoptimizations$model();

    void txoptimizations$setModel(Identifier id, Map<Identifier, ItemModel> map, ItemModel model);

    Identifier txoptimizations$propertiesId();

    Map<Identifier, ClientItem.Properties> txoptimizations$propertiesMap();

    ClientItem.Properties txoptimizations$properties();

    void txoptimizations$setProperties(Identifier id, Map<Identifier, ClientItem.Properties> map, ClientItem.Properties properties);
}
