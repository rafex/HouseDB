package com.rafex.housedb.bootstrap;

import java.util.Objects;
import java.util.function.Supplier;

final class Lazy<T> {

    private final Supplier<T> supplier;
    private volatile T value;

    Lazy(final Supplier<T> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "supplier");
    }

    T get() {
        T current = value;
        if (current != null) {
            return current;
        }

        synchronized (this) {
            current = value;
            if (current == null) {
                current = supplier.get();
                value = current;
            }
            return current;
        }
    }
}
