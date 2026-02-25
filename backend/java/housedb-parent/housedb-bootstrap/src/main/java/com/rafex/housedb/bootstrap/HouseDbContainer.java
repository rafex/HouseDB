package com.rafex.housedb.bootstrap;

import com.rafex.housedb.db.Db;
import com.rafex.housedb.repository.HouseManagementRepository;
import com.rafex.housedb.repository.HouseLocationSyncRepository;
import com.rafex.housedb.repository.InventoryMutationRepository;
import com.rafex.housedb.repository.InventorySearchRepository;
import com.rafex.housedb.repository.impl.HouseRepositoryImpl;
import com.rafex.housedb.repository.impl.ItemRepositoryImpl;
import com.rafex.housedb.services.HouseService;
import com.rafex.housedb.services.ItemFinderService;
import com.rafex.housedb.services.impl.HouseServiceImpl;
import com.rafex.housedb.services.impl.ItemFinderServiceImpl;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.sql.DataSource;

public final class HouseDbContainer {

    public record Overrides(Optional<Supplier<HouseDbConfig>> config, Optional<Supplier<DataSource>> dataSource,
            Optional<Supplier<InventorySearchRepository>> inventorySearchRepository,
            Optional<Supplier<InventoryMutationRepository>> inventoryMutationRepository,
            Optional<Supplier<HouseLocationSyncRepository>> houseLocationSyncRepository,
            Optional<Supplier<ItemFinderService>> itemFinderService,
            Optional<Supplier<HouseManagementRepository>> houseManagementRepository,
            Optional<Supplier<HouseService>> houseService) {
        public Overrides {
            config = config != null ? config : Optional.empty();
            dataSource = dataSource != null ? dataSource : Optional.empty();
            inventorySearchRepository = inventorySearchRepository != null ? inventorySearchRepository : Optional.empty();
            inventoryMutationRepository = inventoryMutationRepository != null
                    ? inventoryMutationRepository
                    : Optional.empty();
            houseLocationSyncRepository = houseLocationSyncRepository != null
                    ? houseLocationSyncRepository
                    : Optional.empty();
            itemFinderService = itemFinderService != null ? itemFinderService : Optional.empty();
            houseManagementRepository = houseManagementRepository != null
                    ? houseManagementRepository
                    : Optional.empty();
            houseService = houseService != null ? houseService : Optional.empty();
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {

            private Supplier<HouseDbConfig> config;
            private Supplier<DataSource> dataSource;
            private Supplier<InventorySearchRepository> inventorySearchRepository;
            private Supplier<InventoryMutationRepository> inventoryMutationRepository;
            private Supplier<HouseLocationSyncRepository> houseLocationSyncRepository;
            private Supplier<ItemFinderService> itemFinderService;
            private Supplier<HouseManagementRepository> houseManagementRepository;
            private Supplier<HouseService> houseService;

            public Builder config(final Supplier<HouseDbConfig> value) {
                config = value;
                return this;
            }

            public Builder dataSource(final Supplier<DataSource> value) {
                dataSource = value;
                return this;
            }

            public Builder inventorySearchRepository(final Supplier<InventorySearchRepository> value) {
                inventorySearchRepository = value;
                return this;
            }

            public Builder inventoryMutationRepository(final Supplier<InventoryMutationRepository> value) {
                inventoryMutationRepository = value;
                return this;
            }

            public Builder houseLocationSyncRepository(final Supplier<HouseLocationSyncRepository> value) {
                houseLocationSyncRepository = value;
                return this;
            }

            public Builder itemFinderService(final Supplier<ItemFinderService> value) {
                itemFinderService = value;
                return this;
            }

            public Builder houseManagementRepository(final Supplier<HouseManagementRepository> value) {
                houseManagementRepository = value;
                return this;
            }

            public Builder houseService(final Supplier<HouseService> value) {
                houseService = value;
                return this;
            }

            public Overrides build() {
                return new Overrides(Optional.ofNullable(config), Optional.ofNullable(dataSource),
                        Optional.ofNullable(inventorySearchRepository), Optional.ofNullable(inventoryMutationRepository),
                        Optional.ofNullable(houseLocationSyncRepository), Optional.ofNullable(itemFinderService),
                        Optional.ofNullable(houseManagementRepository), Optional.ofNullable(houseService));
            }
        }
    }

    private final Lazy<HouseDbConfig> config;
    private final Lazy<DataSource> dataSource;
    private final Lazy<ItemRepositoryImpl> itemRepository;
    private final Lazy<InventorySearchRepository> inventorySearchRepository;
    private final Lazy<InventoryMutationRepository> inventoryMutationRepository;
    private final Lazy<HouseLocationSyncRepository> houseLocationSyncRepository;
    private final Lazy<ItemFinderService> itemFinderService;
    private final Lazy<HouseManagementRepository> houseManagementRepository;
    private final Lazy<HouseService> houseService;
    private final Lazy<HouseRepositoryImpl> houseRepository;

    public HouseDbContainer() {
        this(Overrides.builder().build());
    }

    public HouseDbContainer(final Overrides overrides) {
        Objects.requireNonNull(overrides, "overrides");

        config = new Lazy<>(select(overrides.config(), HouseDbConfig::fromEnv));
        dataSource = new Lazy<>(select(overrides.dataSource(), DataSourceFactory::create));
        itemRepository = new Lazy<>(() -> new ItemRepositoryImpl(dataSource()));
        houseRepository = new Lazy<>(() -> new HouseRepositoryImpl(dataSource()));

        inventorySearchRepository = new Lazy<>(select(overrides.inventorySearchRepository(),
                this::itemRepository));
        inventoryMutationRepository = new Lazy<>(select(overrides.inventoryMutationRepository(),
                this::itemRepository));
        houseLocationSyncRepository = new Lazy<>(select(overrides.houseLocationSyncRepository(),
                this::itemRepository));

        itemFinderService = new Lazy<>(select(overrides.itemFinderService(),
                () -> new ItemFinderServiceImpl(inventorySearchRepository(), inventoryMutationRepository(),
                        houseLocationSyncRepository())));
        houseManagementRepository = new Lazy<>(select(overrides.houseManagementRepository(),
                this::houseRepository));
        houseService = new Lazy<>(select(overrides.houseService(),
                () -> new HouseServiceImpl(houseManagementRepository())));
    }

    public HouseDbConfig config() {
        return config.get();
    }

    public DataSource dataSource() {
        return dataSource.get();
    }

    public InventorySearchRepository inventorySearchRepository() {
        return inventorySearchRepository.get();
    }

    private ItemRepositoryImpl itemRepository() {
        return itemRepository.get();
    }

    public InventoryMutationRepository inventoryMutationRepository() {
        return inventoryMutationRepository.get();
    }

    public HouseLocationSyncRepository houseLocationSyncRepository() {
        return houseLocationSyncRepository.get();
    }

    public ItemFinderService itemFinderService() {
        return itemFinderService.get();
    }

    private HouseRepositoryImpl houseRepository() {
        return houseRepository.get();
    }

    public HouseManagementRepository houseManagementRepository() {
        return houseManagementRepository.get();
    }

    public HouseService houseService() {
        return houseService.get();
    }

    public void warmup() {
        config();
        dataSource();
        inventorySearchRepository();
        inventoryMutationRepository();
        houseLocationSyncRepository();
        itemFinderService();
        houseManagementRepository();
        houseService();
    }

    private static <T> Supplier<T> select(final Optional<Supplier<T>> override, final Supplier<T> def) {
        return override.orElse(def);
    }

    public static final class HouseDbConfig {

        private final int defaultSearchLimit;

        private HouseDbConfig(final int defaultSearchLimit) {
            this.defaultSearchLimit = defaultSearchLimit;
        }

        public static HouseDbConfig fromEnv() {
            final var raw = System.getenv("HOUSEDB_DEFAULT_SEARCH_LIMIT");
            if (raw == null || raw.isBlank()) {
                return new HouseDbConfig(50);
            }

            try {
                final int parsed = Integer.parseInt(raw.trim());
                return new HouseDbConfig(Math.max(1, Math.min(parsed, 200)));
            } catch (final NumberFormatException ignored) {
                return new HouseDbConfig(50);
            }
        }

        public int defaultSearchLimit() {
            return defaultSearchLimit;
        }
    }

    public static final class DataSourceFactory {

        private DataSourceFactory() {
        }

        public static DataSource create() {
            return Db.dataSource();
        }
    }
}
