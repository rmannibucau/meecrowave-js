package com.github.rmannibucau.meecrowave.js.spi;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import org.graalvm.polyglot.Value;

public interface BindingContributor {
    String ATTRIBUTE = BindingContributor.class.getName() + "s";

    default void init() {
        // no-op
    }

    void contribute(final Value bindings);

    default void destroy() {
        // no-op
    }

    static Collection<BindingContributor> load() {
        return StreamSupport.stream(ServiceLoader.load(BindingContributor.class).spliterator(), false).collect(toList());
    }
}
