package com.github.rmannibucau.meecrowave.js.io;

import java.io.IOException;
import java.io.InputStream;

import com.github.rmannibucau.meecrowave.js.fn.IOSupplier;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DelegatingInputStream extends InputStream {
    private final IOSupplier<InputStream> delegate;

    @Override
    public int read(final byte[] b) throws IOException {
        return delegate.get().read(b);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return delegate.get().read(b, off, len);
    }

    @Override
    public long skip(final long n) throws IOException {
        return delegate.get().skip(n);
    }

    @Override
    public int available() throws IOException {
        return delegate.get().available();
    }

    @Override
    public void close() throws IOException {
        delegate.get().close();
    }

    @Override
    public synchronized void mark(final int readlimit) {
        try {
            delegate.get().mark(readlimit);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        delegate.get().reset();
    }

    @Override
    public boolean markSupported() {
        try {
            return delegate.get().markSupported();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int read() throws IOException {
        return delegate.get().read();
    }
}
