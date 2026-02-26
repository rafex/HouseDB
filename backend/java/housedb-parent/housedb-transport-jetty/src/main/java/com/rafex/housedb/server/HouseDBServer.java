package com.rafex.housedb.server;

import com.rafex.housedb.bootstrap.HouseDbContainer;
import com.rafex.housedb.handlers.HealthHandler;
import com.rafex.housedb.handlers.JwtAuthHandler;
import com.rafex.housedb.handlers.LoginHandler;
import com.rafex.housedb.handlers.houses.HousesRouterHandler;
import com.rafex.housedb.handlers.NotFoundHandler;
import com.rafex.housedb.handlers.TokenHandler;
import com.rafex.housedb.handlers.items.ItemsRouterHandler;
import com.rafex.housedb.handlers.users.UsersRouterHandler;
import com.rafex.housedb.json.JsonUtil;
import com.rafex.housedb.kiwi.KiwiApiClient;
import com.rafex.housedb.security.JwtService;

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
        final var jwt = new JwtService(JsonUtil.MAPPER, System.getenv().getOrDefault("JWT_ISS", "com.rafex.housedb"),
                System.getenv().getOrDefault("JWT_AUD", "housedb-backend"),
                System.getenv().getOrDefault("JWT_SECRET", "CHANGE_ME_NOW_32+chars_secret"));
        routes.addMapping(PathSpec.from("/health"), new HealthHandler());
        routes.addMapping(PathSpec.from("/auth/login"), new LoginHandler(jwt, container.authService()));
        routes.addMapping(PathSpec.from("/auth/token"), new TokenHandler(jwt, container.appClientAuthService()));
        final var kiwiApiClient = new KiwiApiClient();
        final var itemRoutes = new ItemsRouterHandler(container.itemFinderService(), kiwiApiClient);
        final var houseRoutes = new HousesRouterHandler(container.houseService(), container.itemFinderService(),
                kiwiApiClient);
        final var userRoutes = new UsersRouterHandler(container.userRepository(), container.passwordHasherPBKDF2());
        routes.addMapping(PathSpec.from("/items"), itemRoutes);
        routes.addMapping(PathSpec.from("/items/*"), itemRoutes);
        routes.addMapping(PathSpec.from("/item/*"), itemRoutes);
        routes.addMapping(PathSpec.from("/houses"), houseRoutes);
        routes.addMapping(PathSpec.from("/houses/*"), houseRoutes);
        routes.addMapping(PathSpec.from("/users"), userRoutes);
        routes.addMapping(PathSpec.from("/users/*"), userRoutes);
        routes.addMapping(PathSpec.from("/*"), new NotFoundHandler());

        final var auth = new JwtAuthHandler(routes, jwt)
                .publicPath("GET", "/health")
                .publicPath("POST", "/auth/login")
                .publicPath("POST", "/auth/token")
                .protectedPrefix("/items")
                .protectedPrefix("/items/*")
                .protectedPrefix("/item/*")
                .protectedPrefix("/houses")
                .protectedPrefix("/houses/*")
                .protectedPrefix("/users")
                .protectedPrefix("/users/*");

        server.setHandler(auth);

        LOG.info("Starting HouseDB backend on port " + port);
        server.start();
        server.join();
    }
}
