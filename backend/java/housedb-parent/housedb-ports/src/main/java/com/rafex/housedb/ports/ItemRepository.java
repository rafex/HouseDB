package com.rafex.housedb.ports;

import com.rafex.housedb.common.HouseItem;

import java.util.List;
import java.util.Optional;

public interface ItemRepository {

    Optional<HouseItem> findByName(String name);

    List<HouseItem> findByCategory(String category);
}
