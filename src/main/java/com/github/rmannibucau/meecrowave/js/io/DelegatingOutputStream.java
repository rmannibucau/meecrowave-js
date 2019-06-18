package com.github.rmannibucau.meecrowave.js.io;

import java.io.IOException;
import java.io.OutputStream;

import com.github.rmannibucau.meecrowave.js.fn.IOSupplier;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DelegatingOutputStream extends OutputStream {
    private final IOSupplier<OutputStream> delegate;

    @Override
    public void write(byte[] b) throws IOException {
        delegate.get().write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        delegate.get().write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        delegate.get().flush();
    }

    @Override
    public void close() throws IOException {
        delegate.get().close();
    }

    @Override
    public void write(final int b) throws IOException {
        delegate.get().write(b);
    }
}
