package com.rafex.housedb.server;

import com.rafex.housedb.bootstrap.HouseDbContainer;
import com.rafex.housedb.handlers.AuthRouterHandler;
import com.rafex.housedb.handlers.HealthHandler;
import com.rafex.housedb.handlers.HelloHandler;
import com.rafex.housedb.handlers.items.ItemAliasRouterHandler;
import com.rafex.housedb.handlers.items.ItemsRouterHandler;
import com.rafex.housedb.handlers.support.GlowrootMiddleware;
import com.rafex.housedb.handlers.support.NotFoundResource;
import com.rafex.housedb.handlers.houses.HousesRouterHandler;
import com.rafex.housedb.handlers.users.UsersRouterHandler;
import com.rafex.housedb.kiwi.KiwiApiClient;
import com.rafex.housedb.security.JwtService;

import java.util.List;
import java.util.logging.Logger;

import dev.rafex.ether.http.core.AuthPolicy;
import dev.rafex.ether.http.jetty12.JettyMiddleware;
import dev.rafex.ether.http.jetty12.JettyRouteRegistry;
import dev.rafex.ether.http.jetty12.JettyServerConfig;
import dev.rafex.ether.http.jetty12.JettyServerFactory;
import dev.rafex.ether.http.jetty12.TokenVerificationResult;
import dev.rafex.ether.json.JsonCodecBuilder;

public final class HouseDBServer {

    private static final Logger LOG = Logger.getLogger(HouseDBServer.class.getName());

    private HouseDBServer() {
    }

    public static void start(final HouseDbContainer container) throws Exception {
        final var jsonCodec = JsonCodecBuilder.create().build();
        final var jwt = new JwtService(jsonCodec.mapper(), System.getenv().getOrDefault("JWT_ISS", "com.rafex.housedb"),
                System.getenv().getOrDefault("JWT_AUD", "housedb-backend"),
                System.getenv().getOrDefault("JWT_SECRET", "CHANGE_ME_NOW_32+chars_secret"));

        final var kiwiApiClient = new KiwiApiClient(jsonCodec);
        kiwiApiClient.bootstrapAppClientFromEnv();

        final var helloHandler = new HelloHandler(jsonCodec);
        final var authRoutes = new AuthRouterHandler(jsonCodec, jwt, container.authService(),
                container.appClientAuthService());
        final var itemRoutes = new ItemsRouterHandler(jsonCodec, container.itemFinderService(), kiwiApiClient);
        final var itemAliasRoutes = new ItemAliasRouterHandler(jsonCodec, kiwiApiClient, container.itemFinderService());
        final var houseRoutes = new HousesRouterHandler(jsonCodec, container.houseService(), container.itemFinderService(),
                kiwiApiClient);
        final var userRoutes = new UsersRouterHandler(jsonCodec, container.userRepository(),
                container.passwordHasherPBKDF2());

        final var routeRegistry = new JettyRouteRegistry();
        routeRegistry.add("/health", new HealthHandler(jsonCodec));
        routeRegistry.add("/hello", helloHandler);
        routeRegistry.add("/hello/name", helloHandler);
        routeRegistry.add("/auth/*", authRoutes);
        routeRegistry.add("/items", itemRoutes);
        routeRegistry.add("/items/*", itemRoutes);
        routeRegistry.add("/item/*", itemAliasRoutes);
        routeRegistry.add("/houses", houseRoutes);
        routeRegistry.add("/houses/*", houseRoutes);
        routeRegistry.add("/users", userRoutes);
        routeRegistry.add("/users/*", userRoutes);
        routeRegistry.add("/*", new NotFoundResource(jsonCodec));

        final var tokenVerifier = (dev.rafex.ether.http.jetty12.TokenVerifier) (token, epochSeconds) -> {
            final var result = jwt.verify(token, epochSeconds);
            if (!result.ok()) {
                return TokenVerificationResult.failed(result.code());
            }
            return TokenVerificationResult.ok(result.ctx());
        };

        final var authPolicies = List.of(
                AuthPolicy.publicPath("GET", "/health"),
                AuthPolicy.publicPath("GET", "/hello"),
                AuthPolicy.publicPath("GET", "/hello/name"),
                AuthPolicy.publicPath("POST", "/hello/name"),
                AuthPolicy.publicPath("POST", "/auth/login"),
                AuthPolicy.publicPath("POST", "/auth/token"),
                AuthPolicy.protectedPrefix("/items"),
                AuthPolicy.protectedPrefix("/items/*"),
                AuthPolicy.protectedPrefix("/item/*"),
                AuthPolicy.protectedPrefix("/houses"),
                AuthPolicy.protectedPrefix("/houses/*"),
                AuthPolicy.protectedPrefix("/users"),
                AuthPolicy.protectedPrefix("/users/*"));

        final List<JettyMiddleware> middlewares = List.of(new GlowrootMiddleware());

        final var config = JettyServerConfig.fromEnv();
        final var runner = JettyServerFactory.create(config, routeRegistry, jsonCodec, tokenVerifier,
                authPolicies, middlewares);

        LOG.info("Starting HouseDB backend on port " + config.port());
        runner.start();
        runner.await();
    }
}
