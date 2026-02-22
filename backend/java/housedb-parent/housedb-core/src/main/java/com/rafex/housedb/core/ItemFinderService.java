package com.rafex.housedb.core;

import com.rafex.housedb.common.HouseItem;
import com.rafex.housedb.ports.ItemRepository;

import java.util.List;
import java.util.Optional;

public final class ItemFinderService {

    private final ItemRepository itemRepository;

    public ItemFinderService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public Optional<HouseItem> findByName(String name) {
        return itemRepository.findByName(name);
    }

    public List<HouseItem> findByCategory(String category) {
        return itemRepository.findByCategory(category);
    }
}
