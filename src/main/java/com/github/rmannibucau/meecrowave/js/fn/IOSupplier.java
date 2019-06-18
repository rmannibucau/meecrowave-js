package com.github.rmannibucau.meecrowave.js.fn;

import java.io.IOException;

public interface IOSupplier<T> {
    T get() throws IOException;
}
