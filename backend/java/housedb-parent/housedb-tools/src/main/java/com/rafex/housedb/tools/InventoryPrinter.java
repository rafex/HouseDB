package com.rafex.housedb.tools;

import com.rafex.housedb.common.HouseItem;

public final class InventoryPrinter {

    public String print(HouseItem houseItem) {
        return houseItem.name() + " (" + houseItem.category() + ") - " + houseItem.location();
    }
}
