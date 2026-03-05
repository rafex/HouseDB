package com.rafex.housedb.handlers;

import com.rafex.housedb.security.JwtService;
import com.rafex.housedb.services.AppClientAuthService;
import com.rafex.housedb.services.AuthService;

import java.util.List;
import java.util.Set;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.http.core.Route;
import dev.rafex.ether.http.jetty12.NonBlockingResourceHandler;
import dev.rafex.ether.json.JsonCodec;

public final class AuthRouterHandler extends NonBlockingResourceHandler {

    private final LoginHandler loginHandler;
    private final TokenHandler tokenHandler;

    public AuthRouterHandler(final JsonCodec jsonCodec, final JwtService jwt, final AuthService authService,
            final AppClientAuthService appClientAuthService) {
        super(jsonCodec);
        this.loginHandler = new LoginHandler(jwt, authService);
        this.tokenHandler = new TokenHandler(jwt, appClientAuthService);
    }

    @Override
    protected String basePath() {
        return "/auth";
    }

    @Override
    protected List<Route> routes() {
        return List.of(
                Route.of("/login", Set.of("POST")),
                Route.of("/token", Set.of("POST")));
    }

    @Override
    public boolean post(final HttpExchange x) throws Exception {
        return switch (x.path()) {
            case "/auth/login" -> loginHandler.handle(x);
            case "/auth/token" -> tokenHandler.handle(x);
            default -> false;
        };
    }
}
