package com.rafex.housedb.handlers.users;

import com.rafex.housedb.repository.UserRepository;
import com.rafex.housedb.security.PasswordHasherPBKDF2;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public final class UsersRouterHandler extends Handler.Abstract {

    private final CreateUserHandler createUserHandler;

    public UsersRouterHandler(final UserRepository userRepository, final PasswordHasherPBKDF2 hasher) {
        createUserHandler = new CreateUserHandler(userRepository, hasher);
    }

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) {
        final String method = request.getMethod();
        final String path = request.getHttpURI().getPath();

        if ("POST".equals(method) && "/users".equals(path)) {
            return createUserHandler.handle(request, response, callback);
        }

        return false;
    }
}
