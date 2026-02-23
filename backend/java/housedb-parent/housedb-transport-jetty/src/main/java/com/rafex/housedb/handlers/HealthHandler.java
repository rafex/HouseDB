package com.rafex.housedb.handlers;

import com.rafex.housedb.http.HttpUtil;

import java.time.Instant;
import java.util.Map;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public final class HealthHandler extends Handler.Abstract.NonBlocking {

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) {
        if (!"/health".equals(request.getHttpURI().getPath())) {
            return false;
        }
        if (request.getMethod() == null || !HttpMethod.GET.is(request.getMethod())) {
            response.setStatus(405);
            callback.succeeded();
            return true;
        }

        HttpUtil.ok(response, callback, Map.of("status", "UP", "timestamp", Instant.now().toString()));
        return true;
    }
}
