package com.rafex.housedb.infra.postgres;

import com.rafex.housedb.common.HouseItem;
import com.rafex.housedb.ports.ItemRepository;

import java.util.List;
import java.util.Optional;

public final class PostgresItemRepository implements ItemRepository {

    @Override
    public Optional<HouseItem> findByName(String name) {
        return Optional.empty();
    }

    @Override
    public List<HouseItem> findByCategory(String category) {
        return List.of();
    }
}
