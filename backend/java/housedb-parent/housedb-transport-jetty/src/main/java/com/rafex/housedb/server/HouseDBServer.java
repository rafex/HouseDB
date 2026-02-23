package com.rafex.housedb.server;

import com.rafex.housedb.bootstrap.HouseDbContainer;
import com.rafex.housedb.handlers.HealthHandler;
import com.rafex.housedb.handlers.NotFoundHandler;
import com.rafex.housedb.handlers.items.ItemsRouterHandler;

import java.util.logging.Logger;

import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.PathMappingsHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public final class HouseDBServer {

    private static final Logger LOG = Logger.getLogger(HouseDBServer.class.getName());

    private HouseDBServer() {
    }

    public static void start(final HouseDbContainer container) throws Exception {
        final int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        final var pool = new QueuedThreadPool();
        pool.setMaxThreads(Math.max(Runtime.getRuntime().availableProcessors() * 2, 16));
        pool.setMinThreads(4);
        pool.setIdleTimeout(30_000);
        pool.setName("housedb-http");

        final var server = new Server(pool);

        final var connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        final var routes = new PathMappingsHandler();
        routes.addMapping(PathSpec.from("/health"), new HealthHandler());
        final var itemRoutes = new ItemsRouterHandler(container.itemFinderService());
        routes.addMapping(PathSpec.from("/items"), itemRoutes);
        routes.addMapping(PathSpec.from("/items/*"), itemRoutes);
        routes.addMapping(PathSpec.from("/locations/sync/kiwi"), itemRoutes);
        routes.addMapping(PathSpec.from("/*"), new NotFoundHandler());

        server.setHandler(routes);

        LOG.info("Starting HouseDB backend on port " + port);
        server.start();
        server.join();
    }
}
