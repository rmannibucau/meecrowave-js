package com.github.rmannibucau.meecrowave.js;

import static java.util.Collections.list;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.rmannibucau.meecrowave.js.io.DelegatingInputStream;
import com.github.rmannibucau.meecrowave.js.io.DelegatingOutputStream;
import com.github.rmannibucau.meecrowave.js.spi.BindingContributor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

public class GenericContextServlet extends HttpServlet {
    private Collection<BindingContributor> contributors;
    private Engine engine;
    private Source source;
    private BiFunction<HttpServletRequest, HttpServletResponse, Context> contextFactory;

    @Override
    public void init() {
        contributors = Collection.class.cast(getServletContext().getAttribute(BindingContributor.ATTRIBUTE));
        engine = Engine.create();
        source = Source.create(
                ofNullable(getServletConfig().getInitParameter("language")).orElse("js"),
                ofNullable(loadResource(getServletConfig().getInitParameter("handler")))
                        .orElseGet(() -> getServletConfig().getServletName()));

        final boolean allowAllAccess = getBoolean("allowAllAccess");
        final boolean allowCreateThread = getBoolean("allowCreateThread");
        final boolean allowExperimentalOptions = getBoolean("allowExperimentalOptions");
        final boolean allowIO = getBoolean("allowIO");
        final boolean allowNativeAccess = getBoolean("allowNativeAccess");
        final HostAccess allowHostAccess = getBoolean("allowHostAccess") ? HostAccess.ALL : HostAccess.NONE;
        final PolyglotAccess allowPolyglotAccess = getBoolean("allowPolyglotAccess") ? PolyglotAccess.ALL : PolyglotAccess.NONE;
        final Map<String, String> options = list(getServletConfig().getInitParameterNames()).stream()
                .filter(it -> it.startsWith("option."))
                .collect(toMap(it -> it.substring("option.".length()), it -> getServletConfig().getInitParameter(it)));
        contextFactory = (req, resp) -> {
            final Context.Builder builder = Context.newBuilder().engine(this.engine);
            builder.in(new DelegatingInputStream(req::getInputStream));
            builder.out(new DelegatingOutputStream(resp::getOutputStream));
            builder.options(options);
            if (allowAllAccess) {
                builder.allowAllAccess(true);
            } else {
                builder.allowCreateThread(allowCreateThread);
                builder.allowExperimentalOptions(allowExperimentalOptions);
                builder.allowHostAccess(allowHostAccess);
                builder.allowIO(allowIO);
                builder.allowNativeAccess(allowNativeAccess);
                builder.allowPolyglotAccess(allowPolyglotAccess);
            }
            return builder.build();
        };
    }

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) {
        try (final Context context = contextFactory.apply(req, resp)) {
            final Value bindings = context.getBindings("js");
            bindings.putMember("request", req);
            bindings.putMember("response", resp);
            contributors.forEach(it -> it.contribute(bindings));
            context.eval(source);
        }
    }

    @Override
    public void destroy() {
        engine.close();
    }

    private String loadResource(final String handler) {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(requireNonNull(
                getServletContext().getClassLoader().getResourceAsStream(handler), "No handler '" + handler + "', found")))) {
            return reader.lines().collect(joining("\n"));
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private boolean getBoolean(final String key) {
        return Boolean.parseBoolean(ofNullable(getServletConfig().getInitParameter(key)).orElse("true"));
    }
}
