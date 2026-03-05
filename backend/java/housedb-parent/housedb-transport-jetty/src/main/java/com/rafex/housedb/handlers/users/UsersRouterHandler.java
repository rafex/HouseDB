package com.rafex.housedb.handlers.users;

import com.rafex.housedb.repository.UserRepository;
import com.rafex.housedb.security.PasswordHasherPBKDF2;

import java.util.List;
import java.util.Set;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.http.core.Route;
import dev.rafex.ether.http.jetty12.NonBlockingResourceHandler;
import dev.rafex.ether.json.JsonCodec;

public final class UsersRouterHandler extends NonBlockingResourceHandler {

    private final CreateUserHandler createUserHandler;

    public UsersRouterHandler(final JsonCodec jsonCodec, final UserRepository userRepository, final PasswordHasherPBKDF2 hasher) {
        super(jsonCodec);
        createUserHandler = new CreateUserHandler(userRepository, hasher);
    }

    @Override
    protected String basePath() {
        return "/users";
    }

    @Override
    protected List<Route> routes() {
        return List.of(Route.of("/", Set.of("POST")));
    }

    @Override
    public boolean post(final HttpExchange x) {
        return createUserHandler.handle(x);
    }
}
