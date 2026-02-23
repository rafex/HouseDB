package com.rafex.housedb.bootstrap;

import java.util.ArrayDeque;
import java.util.Deque;

public final class Closer implements AutoCloseable {

    private final Deque<AutoCloseable> closeables = new ArrayDeque<>();

    public void register(final AutoCloseable closeable) {
        if (closeable != null) {
            closeables.push(closeable);
        }
    }

    @Override
    public void close() {
        while (!closeables.isEmpty()) {
            try {
                closeables.pop().close();
            } catch (final Exception ignored) {
                // best effort shutdown
            }
        }
    }
}
