package com.rafex.housedb.handlers.support;

import java.util.Set;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import dev.rafex.ether.http.jetty12.JettyMiddleware;

public final class CorsMiddleware implements JettyMiddleware {

    private static final Set<String> ALLOWED_ORIGINS = Set.of(
            "https://housedb.rafex.app",
            "http://localhost:5173");

    @Override
    public Handler wrap(final Handler next) {
        return new Handler.Wrapper(next) {
            @Override
            public boolean handle(final Request request, final Response response, final Callback callback)
                    throws Exception {
                final var origin = request.getHeaders().get("Origin");
                if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
                    response.getHeaders().put("Access-Control-Allow-Origin", origin);
                    response.getHeaders().put("Vary", "Origin");
                    response.getHeaders().put("Access-Control-Allow-Headers",
                            "Authorization, Content-Type, Accept, Origin");
                    response.getHeaders().put("Access-Control-Allow-Methods",
                            "GET, POST, PUT, PATCH, DELETE, OPTIONS");
                }

                if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                    response.setStatus(204);
                    callback.succeeded();
                    return true;
                }

                return super.handle(request, response, callback);
            }
        };
    }
}
