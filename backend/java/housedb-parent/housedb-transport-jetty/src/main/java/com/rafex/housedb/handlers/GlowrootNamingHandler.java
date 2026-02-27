package com.rafex.housedb.handlers;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.glowroot.agent.api.Glowroot;

public final class GlowrootNamingHandler extends Handler.Wrapper {

    private static final Logger LOG = Logger.getLogger(GlowrootNamingHandler.class.getName());

    private static final Pattern UUID_PATTERN = Pattern
            .compile("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?=/|$)");
    private static final Pattern OBJECT_ID_PATTERN = Pattern.compile("/[0-9a-fA-F]{24}(?=/|$)");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("/\\d{2,}(?=/|$)");
    private static final Pattern MULTIPLE_SLASHES_PATTERN = Pattern.compile("/{2,}");

    public GlowrootNamingHandler(final Handler delegate) {
        super(delegate);
    }

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {
        final var method = request.getMethod();
        final var path = request.getHttpURI() != null ? request.getHttpURI().getPath() : null;
        final var normalizedPath = normalizePath(path);

        setGlowrootAttributes(method, path, normalizedPath);

        try {
            return super.handle(request, response, callback);
        } catch (final Throwable t) {
            try {
                Glowroot.addTransactionAttribute("error", t.getClass().getName());
                Glowroot.addTransactionAttribute("error.message", safeMessage(t.getMessage()));
            } catch (final Throwable ignored) {
                LOG.log(Level.WARNING, "Error setting Glowroot error attributes", ignored);
            }
            throw t;
        }
    }

    static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "unknown";
        }
        path = MULTIPLE_SLASHES_PATTERN.matcher(path).replaceAll("/");
        path = UUID_PATTERN.matcher(path).replaceAll("/:id");
        path = OBJECT_ID_PATTERN.matcher(path).replaceAll("/:id");
        return NUMBER_PATTERN.matcher(path).replaceAll("/:n");
    }

    private static String safeMessage(final String message) {
        return message == null ? "" : message;
    }

    private void setGlowrootAttributes(final String method, final String rawPath, final String normalizedPath) {
        try {
            Glowroot.setTransactionType("Web");
            Glowroot.setTransactionName(method + " " + normalizedPath);
            Glowroot.addTransactionAttribute("http.method", method);
            Glowroot.addTransactionAttribute("http.path", rawPath == null ? "unknown" : rawPath);
            Glowroot.addTransactionAttribute("http.normalized_path", normalizedPath);
        } catch (final Throwable t) {
            LOG.log(Level.WARNING, "Error setting Glowroot transaction attributes", t);
        }
    }
}
