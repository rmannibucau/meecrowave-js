package com.github.rmannibucau.meecrowave.js;

import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;

import com.github.rmannibucau.meecrowave.js.spi.BindingContributor;

public class AddContextServlets implements ServletContainerInitializer {
    @Override
    public void onStartup(final Set<Class<?>> c, final ServletContext ctx) {
        final Collection<BindingContributor> contributors = BindingContributor.load();
        ctx.setAttribute(BindingContributor.ATTRIBUTE, contributors);
        ctx.addListener(new ServletContextListener() {
            @Override
            public void contextInitialized(final ServletContextEvent sce) {
                contributors.forEach(BindingContributor::init);
            }

            @Override
            public void contextDestroyed(final ServletContextEvent sce) {
                contributors.forEach(BindingContributor::destroy);
            }
        });

        final AtomicInteger counter = new AtomicInteger();
        loadConfig(ctx).getJsonArray("routes").stream()
                .map(JsonValue::asJsonObject)
                .forEach(route -> addBinding(ctx, counter, route));
    }

    private void addBinding(final ServletContext ctx, final AtomicInteger counter, final JsonObject route) {
        final String name = ofNullable(route.get("name")).map(this::asString).orElseGet(() -> "route-" + counter.incrementAndGet());
        final ServletRegistration.Dynamic servlet = ctx.addServlet(name, GenericContextServlet.class);
        servlet.setAsyncSupported(true);
        ofNullable(route.get("language")).ifPresent(l -> servlet.setInitParameter("language", asString(l)));
        ofNullable(route.get("handler")).ifPresent(l -> servlet.setInitParameter("handler", asString(l)));
        servlet.setLoadOnStartup(ofNullable(route.get("loadOnStartup")).map(this::asInt).orElse(1));
        ofNullable(route.get("urlPatterns"))
                .map(JsonValue::asJsonArray)
                .map(array -> array.stream().map(this::asString).toArray(String[]::new))
                .ifPresent(servlet::addMapping);
        ofNullable(route.get("options"))
                .map(JsonValue::asJsonObject)
                .ifPresent(options -> options.forEach((k, v) -> servlet.setInitParameter("option." + k, asString(v))));
        setBoolIfSet(route, servlet, "allowAllAccess");
        setBoolIfSet(route, servlet, "allowCreateThread");
        setBoolIfSet(route, servlet, "allowExperimentalOptions");
        setBoolIfSet(route, servlet, "allowIO");
        setBoolIfSet(route, servlet, "allowNativeAccess");
        setBoolIfSet(route, servlet, "allowHostAccess");
        setBoolIfSet(route, servlet, "allowPolyglotAccess");
    }

    private void setBoolIfSet(final JsonObject route, final ServletRegistration.Dynamic servlet, final String key) {
        ofNullable(route.get(key)).map(this::asBool).ifPresent(b -> servlet.setInitParameter(key, Boolean.toString(b)));
    }

    private boolean asBool(final JsonValue jsonValue) {
        return JsonValue.TRUE.equals(jsonValue);
    }

    private int asInt(final JsonValue jsonValue) {
        return JsonNumber.class.cast(jsonValue).intValue();
    }

    private String asString(final JsonValue jsonValue) {
        return JsonString.class.cast(jsonValue).getString();
    }

    private JsonObject loadConfig(final ServletContext context) {
        try (final JsonReader reader = Json.createReaderFactory(singletonMap("org.apache.johnzon.supports-comments", "true"))
                .createReader(getRoutesConfig(context))) {
            return reader.readObject();
        }
    }

    private InputStream getRoutesConfig(final ServletContext context) {
        return ofNullable(context.getInitParameter("routes.location"))
            .map(Paths::get)
            .filter(Files::exists)
            .map(p -> {
                try {
                    return Files.newInputStream(p);
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            })
            .orElseGet(() -> requireNonNull(context.getResourceAsStream("WEB-INF/routes.json"), "missing WEB-INF/routes.json"));
    }
}
