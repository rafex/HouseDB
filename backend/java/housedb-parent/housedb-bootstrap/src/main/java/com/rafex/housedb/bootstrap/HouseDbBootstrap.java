package com.rafex.housedb.bootstrap;

import java.util.Objects;

import com.rafex.housedb.services.ItemFinderService;

public final class HouseDbBootstrap {

    private HouseDbBootstrap() {
    }

    public record HouseDbRuntime(HouseDbContainer container, Closer closer) implements AutoCloseable {

        public HouseDbRuntime {
            Objects.requireNonNull(container, "container");
            Objects.requireNonNull(closer, "closer");
        }

        public ItemFinderService itemFinderService() {
            return container.itemFinderService();
        }

        @Override
        public void close() {
            closer.close();
        }
    }

    public static HouseDbRuntime start() {
        return start(new HouseDbContainer(), true);
    }

    public static HouseDbRuntime start(final HouseDbContainer container, final boolean warmup) {
        Objects.requireNonNull(container, "container");

        final var closer = new Closer();

        if (warmup) {
            container.warmup();
        }

        final var ds = container.dataSource();
        if (ds instanceof final AutoCloseable ac) {
            closer.register(ac);
        }

        final var runtime = new HouseDbRuntime(container, closer);
        Runtime.getRuntime().addShutdownHook(new Thread(runtime::close, "housedb-shutdown"));

        return runtime;
    }
}
