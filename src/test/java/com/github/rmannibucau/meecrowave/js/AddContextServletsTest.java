package com.github.rmannibucau.meecrowave.js;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.junit5.MeecrowaveConfig;
import org.apache.meecrowave.testing.ConfigurationInject;
import org.junit.jupiter.api.Test;

@MeecrowaveConfig
class AddContextServletsTest {
    @ConfigurationInject
    private Meecrowave.Builder config;

    @Test
    void bar() throws IOException {
        assertEquals("{\"uri\":\"/bar/dummy\"}", slurp("/bar/dummy"));
    }

    @Test
    void foo() throws IOException {
        assertEquals("{\"uri\":\"/foo\"}", slurp("/foo"));
    }

    private String slurp(final String path) throws IOException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(
                new URL("http://localhost:" + config.getHttpPort() + path).openStream()))) {
            return reader.lines().findFirst().orElseThrow(AssertionError::new);
        }
    }
}
