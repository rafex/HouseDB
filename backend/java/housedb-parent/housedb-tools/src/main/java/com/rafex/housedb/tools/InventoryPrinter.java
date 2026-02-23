package com.rafex.housedb.tools;

import com.rafex.housedb.service.models.HouseItem;

public final class InventoryPrinter {

    public String print(HouseItem houseItem) {
        return houseItem.objectName() + " [" + houseItem.inventoryItemId() + "] @ " + houseItem.houseLocationPath();
    }
}
