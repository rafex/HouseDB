package com.rafex.housedb.handlers;

import com.rafex.housedb.http.HttpUtil;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public final class NotFoundHandler extends Handler.Abstract {

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) {
        HttpUtil.notFound(response, callback, request.getHttpURI().getPath());
        return true;
    }
}
